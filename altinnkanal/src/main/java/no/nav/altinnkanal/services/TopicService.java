package no.nav.altinnkanal.services;

public interface TopicService extends TopicRepository {
    void updateCache(String serviceCode, String serviceEditionCode) throws Exception;
}
