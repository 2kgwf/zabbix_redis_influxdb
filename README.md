# Zabbix Redis InfluxDB exporter

Exporter for [zabbix_redis_exporter](https://github.com/Scrin/zabbix_redis_exporter) to move data exported from Zabbix to InfluxDB

### Configuration

Configuration can be set with a config file and/or environment variables.

To set the config with a config file, copy `zabbix_redis_influxdb.properties.example` as `zabbix_redis_influxdb.properties` either to the same directory as the jar or the parent directory (=project root when local dev)

To set the config with environment variables, convert the properties to uppercase, replace dots with underscores and prefix them with `ZRI_`. For example, the `zabbix.url` in the config can be set with with `ZRI_ZABBIX_URL` environment variable. All available config properties are in `zabbix_redis_influxdb.properties.example`.
