package fi.tkgwf.zri;

import fi.tkgwf.zri.bean.zabbix.Host;
import fi.tkgwf.zri.bean.zabbix.Item;
import fi.tkgwf.zri.config.Config;
import fi.tkgwf.zri.connection.InfluxDBConnection;
import fi.tkgwf.zri.connection.ZabbixConnection;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger LOG = LogManager.getLogger();

    private static final String REDIS_QUEUE_ITEM_NAME = "zabbix_history";

    private static final long STATS_INTERVAL_MS = 60 * 60 * 1000;

    private ZabbixConnection zabbix;
    private InfluxDBConnection influx;
    private RedisClient redis;
    private StatefulRedisConnection<String, String> redisConnection;
    private long nextStats;
    private long processed;
    private long skipped;

    public static void main(String[] args) {
        Main m = new Main();
        LOG.info("Initializing...");
        m.init();
        LOG.info("Initialization done");
        m.run();
        LOG.info("Main loop ended, cleaning up...");
        m.cleanup();
        LOG.info("Clean exit");
        System.exit(0); // workaround for https://github.com/influxdata/influxdb-java/issues/359
    }

    private void init() {
        zabbix = new ZabbixConnection();
        influx = new InfluxDBConnection();
        redis = RedisClient.create("redis://" + Config.get("redis.host") + ":" + Config.get("redis.port"));
        redisConnection = redis.connect();
        nextStats = System.currentTimeMillis() + STATS_INTERVAL_MS;
        processed = 0;
        skipped = 0;
    }

    private void run() {
        while (true) {
            try {
                String entry = redisConnection.sync().lpop(REDIS_QUEUE_ITEM_NAME);
                if (entry == null) {
                    try {
                        Thread.sleep(2000); // empty queue, wait for a bit
                        continue;
                    } catch (InterruptedException ex) {
                        LOG.info("Interrupted. Exiting...", ex);
                        break;
                    }
                }
                boolean success = processEntry(entry);
                if (!success) {
                    try {
                        Thread.sleep(10000); // possibly lost connection or so, wait for a bit
                    } catch (InterruptedException ex) {
                        LOG.info("Interrupted. Exiting...", ex);
                        break;
                    }
                }
                if (System.currentTimeMillis() > nextStats) {
                    logStats();
                }
            } catch (Exception ex) {
                LOG.error("Unexpected error while processing data, data will be lost!", ex);
            }
        }
    }

    private void cleanup() {
        influx.close();
        redisConnection.close();
        redis.shutdown();
    }

    /**
     * Process a single entry
     *
     * @param entry
     * @return true if the entry was either processed or discarded as expected,
     * false in case of unexpected errors that should trigger a cooldown
     */
    private boolean processEntry(String entry) {
        String[] split = entry.split(" ", 5);
        if (split.length != 5) {
            LOG.warn("Malformed data: " + entry);
            return true;
        }
        Long timeSeconds = safeLong(split[0]);
        Long timeNanos = safeLong(split[1]);
        String itemId = split[2];
        String valueType = split[3];
        String valueString = split[4];
        if (timeSeconds == null || timeNanos == null) {
            LOG.warn("Malformed data: " + entry);
            skipped++;
            return true;
        }
        Double value;
        switch (valueType) {
            case "i": // InfluxDB doesn't handle cases where the item type has changed, so safer to just treat everything as floats
            case "f":
                value = Double.valueOf(valueString);
                break;
            case "s":
            case "t":
            case "l":
                skipped++;
                return true; // we're not interested in other types, just discard them
            default:
                LOG.warn("Malformed data: " + entry);
                skipped++;
                return true;
        }
        try {
            Item item = zabbix.getItem(itemId);
            if (item != null) {
                Host host = zabbix.getHost(item.hostid);
                if (host != null) {
                    long timestamp = timeSeconds * 1_000_000_000 + timeNanos;
                    influx.save(host, item, timestamp, value);
                    processed++;
                    return true;
                } else {
                    LOG.error("Can't get gost details for host id " + item.hostid + " (for item id " + itemId + ")");
                }
            } else {
                LOG.error("Can't get item details for item id " + itemId);
            }
        } catch (IOException ex) {
            LOG.error("Failed to get item from zabbix", ex);
        }
        handleFailure(entry);
        return false;
    }

    private void logStats() {
        long durationMs = System.currentTimeMillis() - nextStats + STATS_INTERVAL_MS;
        double durationH = durationMs / (60 * 60 * 1000.0);
        double processedPerSecond = processed * 1000.0 / durationMs;
        double skippedPerSecond = skipped * 1000.0 / durationMs;
        LOG.info("Processed " + processed + " and skipped " + skipped + " items in the past " + durationH + " hours (processed: " + processedPerSecond + "/sec, skipped: " + skippedPerSecond + "/sec)");
        nextStats = System.currentTimeMillis() + STATS_INTERVAL_MS;
        processed = 0;
        skipped = 0;
    }

    private void handleFailure(String data) {
        LOG.info("Returning failed data to queue " + REDIS_QUEUE_ITEM_NAME + ": " + data);
        redisConnection.sync().lpush(REDIS_QUEUE_ITEM_NAME, data);
    }

    private Long safeLong(String s) {
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
