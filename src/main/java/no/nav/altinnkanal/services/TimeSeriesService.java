package no.nav.altinnkanal.services;

import no.nav.altinnkanal.RoutingStatus;

public interface TimeSeriesService {
    void logKafkaPublishTime(Long publishTime, int dataSize);
    void logKafkaPublishStatus(String serviceCode, String serviceEditionCode, RoutingStatus routingStatus);
}
