package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMappingUpdate;

import java.sql.SQLException;
import java.util.List;

public interface LogService {
    TopicMappingUpdate logChange(TopicMappingUpdate topicMappingUpdate) throws SQLException;
    List<TopicMappingUpdate> getUniqueChangelog(Boolean enabled) throws SQLException;
    List<TopicMappingUpdate> getChangeLogFor(String serviceCode, String serviceEditionCode) throws SQLException;
    TopicMappingUpdate getLastChangeFor(String serviceCode, String serviceEditionCode) throws SQLException;
}
