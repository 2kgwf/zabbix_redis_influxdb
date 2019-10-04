package fi.tkgwf.zri.connection;

import fi.tkgwf.zri.bean.zabbix.Host;
import fi.tkgwf.zri.bean.zabbix.Item;
import fi.tkgwf.zri.config.Config;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

public class InfluxDBConnection {

    private final InfluxDB influxDB;

    public InfluxDBConnection() {
        String url = Config.get("influxdb.url");
        String user = Config.get("influxdb.user");
        String password = Config.get("influxdb.password");
        String database = Config.get("influxdb.database");
        String retentionPolicy = Config.get("influxdb.retentionpolicy");
        boolean gzip = Config.getBoolean("influxdb.gzip", true);
        boolean batch = Config.getBoolean("influxdb.batch.enable", true);
        int batchSize = Config.getInt("influxdb.batch.size", 1000);
        int batchTime = Config.getInt("influxdb.batch.time", 10000);

        influxDB = InfluxDBFactory.connect(url, user, password).setDatabase(database).setRetentionPolicy(retentionPolicy);
        if (gzip) {
            influxDB.enableGzip();
        } else {
            influxDB.disableGzip();
        }
        if (batch) {
            influxDB.enableBatch(batchSize, batchTime, TimeUnit.MILLISECONDS);
        } else {
            influxDB.disableBatch();
        }
    }

    public void save(Host host, Item item, Long timestamp, Number value) {
        Point entry = toPoint(host, item, timestamp, value);
        influxDB.write(entry);
    }

    public void close() {
        influxDB.close();
    }

    private Point toPoint(Host host, Item item, Long timestamp, Number value) {
        return Point.measurement(item.key_.split("\\[", 2)[0])
                .time(timestamp, TimeUnit.NANOSECONDS)
                .tag("item", item.name)
                .tag("host", host.host)
                .tag("itemid", item.itemid)
                .tag("hostid", host.hostid)
                .tag("key", item.key_)
                .addField("value", value)
                .build();
    }
}
