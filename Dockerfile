FROM maven:alpine as build

RUN mkdir /src
WORKDIR /src
COPY . /src
RUN mvn package

FROM openjdk:8-jre-alpine

COPY --from=build /src/target/zabbix_redis_influxdb-*.jar /zri.jar

CMD java -jar /zri.jar
