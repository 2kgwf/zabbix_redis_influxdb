package fi.tkgwf.zri.config;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Config {

    private static final Logger LOG = LogManager.getLogger();
    private static final Map<String, String> CONFIG = new HashMap<>();
    private static final String ENV_PREFIX = "ZRI_";

    static {
        createDefaultConfig();
        readConfig();
    }

    /**
     * Get the value for specified config key
     *
     * @param key
     * @return the value, or null if it's not specified
     */
    public static String get(String key) {
        return CONFIG.get(key);
    }

    /**
     * Get the value for specified config key as an int
     *
     * @param key
     * @param defaultValue Value to return if the specified config value is not
     * an int or it is unset
     * @return the value, or defaultValue if the value is not specified
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (NullPointerException | NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Get the value for specified config key as a boolean
     *
     * @param key
     * @param defaultValue Value to return if the specified config value is not
     * a boolean or it is unset
     * @return the value, or defaultValue if the value is not specified
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key);
        if ("true".equalsIgnoreCase(v)) {
            return true;
        } else if ("false".equalsIgnoreCase(v)) {
            return false;
        } else {
            return defaultValue;
        }
    }

    private static void createDefaultConfig() {
        CONFIG.put("zabbix.url", "http://localhost");
        CONFIG.put("zabbix.user", null);
        CONFIG.put("zabbix.password", null);
        CONFIG.put("redis.host", "localhost");
        CONFIG.put("redis.port", "6379");
        CONFIG.put("influxdb.url", "http://localhost:8086");
        CONFIG.put("influxdb.user", "zabbix");
        CONFIG.put("influxdb.password", "zabbix");
        CONFIG.put("influxdb.database", "zabbix");
        CONFIG.put("influxdb.retentionpolicy", "autogen");
        CONFIG.put("influxdb.measurement", "zabbix");
        CONFIG.put("influxdb.gzip", "true");
        CONFIG.put("influxdb.batch.enable", "true");
        CONFIG.put("influxdb.batch.size", "1000");
        CONFIG.put("influxdb.batch.time", "10000");
    }

    private static void readConfig() {
        try {
            FileFilter configFileFilter = f -> f.isFile() && f.getName().equals("zabbix_redis_influxdb.properties");
            File jarLocation = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
            File[] configFiles = jarLocation.listFiles(configFileFilter);
            if (configFiles == null || configFiles.length == 0) {
                // look for config files in the parent directory if none found in the current directory
                configFiles = jarLocation.getParentFile().listFiles(configFileFilter);
            }
            if (configFiles != null && configFiles.length > 0) {
                LOG.debug("Config: " + configFiles[0]);
                Properties props = new Properties();
                props.load(new FileInputStream(configFiles[0]));
                Enumeration<?> e = props.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = props.getProperty(key);
                    CONFIG.put(key, value);
                }
            }
            System.getenv().forEach((key, value) -> {
                if (key.startsWith(ENV_PREFIX)) {
                    CONFIG.put(key.substring(ENV_PREFIX.length()).toLowerCase().replace("_", "."), value);
                }
            });
        } catch (URISyntaxException | IOException ex) {
            LOG.warn("Failed to read configuration, using default values...", ex);
        }
    }
}
