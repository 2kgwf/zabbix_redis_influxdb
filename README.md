# Zabbix Redis InfluxDB exporter

Exporter for [zabbix_redis_exporter](https://github.com/Scrin/zabbix_redis_exporter) to read raw history export from Redis, enrich it and store to InfluxDB for long-term storage. Latest version available on Dockerhub: [scrinii/zabbix_redis_influxdb](https://hub.docker.com/r/scrinii/zabbix_redis_influxdb)

The raw history export from Zabbix contains only the timestamp, item ID, value and type, which is not really fun to work with. The purpose of this "exporter for the exporter" is to "enrich" the raw data by resolving the actual item name, key and associated host from the Zabbix instance using the Zabbix API and then store the numeric values into InfluxDB for long-term storage.

The reason for separating this process into two pieces (with Redis in between) was because I wanted to have a consistent and as-realtime-as-possible export without having a noticeable impact on the Zabbix server peformance, and the easiest way to do that was to create a loadable module for the server which simply pushes the data into Redis, and then another separate program that doesn't even need to run on the same server to do all the "heavy lifting" to actually get the data into InfluxDB in an useful format.

## Configuration

Configuration can be set with a config file and/or environment variables. Environment variables are the preferred way when running in a docker container. Config file is usually more convenient during development.

To set the config with a config file, copy `zabbix_redis_influxdb.properties.example` as `zabbix_redis_influxdb.properties` either to the same directory as the jar or the parent directory (=project root when local dev)

To set the config with environment variables, convert the properties to uppercase, replace dots with underscores and prefix them with `ZRI_`. For example, the `zabbix.url` in the config can be set with with `ZRI_ZABBIX_URL` environment variable. All available config properties are in `zabbix_redis_influxdb.properties.example`.

There's a sample `docker-compose.yml` with some example configuration you can use. You shouldn't use it as-is for production use without modifications because it's a relatively minimalistic setup that doesn't have defined volumes for example.

## Development

### Building

Development requirements:
- JDK8
- Maven

Building the final jar package:

```sh
mvn package
```

Will create a `zabbix_redis_influxdb-*.jar` in the `target` directory.

Compile and execute without building a package (useful during development):

```sh
mvn compile exec:java
```
