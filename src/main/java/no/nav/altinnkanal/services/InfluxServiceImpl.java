package no.nav.altinnkanal.services;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
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
    public void logFailedDisabled(String serviceCode, String serviceEditionCode) {
        influxDB.write(Point.measurement("altinnkanal")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("service_code", serviceCode)
                .addField("service_edition_code", serviceEditionCode)
                .addField("status", "failed_disabled")
                .build());
    }

    @Override
    public void logFailedMissing(String serviceCode, String serviceEditionCode) {
    }

    @Override
    public void logSuccessful(String serviceCode, String serviceEditionCode) {

    }
}
