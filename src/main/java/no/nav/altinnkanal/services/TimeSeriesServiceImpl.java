package no.nav.altinnkanal.services;

import no.nav.altinnkanal.RoutingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TimeSeriesServiceImpl implements TimeSeriesService {


    @Autowired
    public TimeSeriesServiceImpl() {

    }

    @Override
    public void logKafkaPublishTime(Long publishTime, int dataSize) {

    }

    @Override
    public void logKafkaPublishStatus(String serviceCode, String serviceEditionCode, RoutingStatus routingStatus) {

    }
}
