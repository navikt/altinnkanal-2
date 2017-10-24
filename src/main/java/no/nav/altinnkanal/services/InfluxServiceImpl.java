package no.nav.altinnkanal.services;

import no.nav.altinnkanal.RoutingStatus;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class InfluxServiceImpl implements InfluxService {
    private final InfluxDB influxDB;

    @Autowired
    public InfluxServiceImpl(InfluxDB influxDB) {
        this.influxDB = influxDB;
    }

    @Override
    public void logKafkaPublishTime(Long publishTime, int dataSize) {
        influxDB.write(Point.measurement("kafka_publish_time")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("publish_time_ms", publishTime)
                .addField("data_size", dataSize)
                .build());
    }

    @Override
    public void logKafkaPublishStatus(String serviceCode, String serviceEditionCode, RoutingStatus routingStatus) {
        influxDB.write(Point.measurement("kafka_published")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("service_code", serviceCode)
                .addField("service_edition_code", serviceEditionCode)
                .addField("status", routingStatus.getInfluxName())
                .build());
    }
}
