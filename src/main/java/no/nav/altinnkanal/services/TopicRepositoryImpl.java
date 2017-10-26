package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Service
public class TopicRepositoryImpl implements TopicRepository {
    private final JdbcTemplate jdbc;

    @Autowired
    public TopicRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception {
        return jdbc.query("SELECT * FROM `topic_mappings` WHERE `service_code`=? AND `service_edition_code`=?;",
              new String[] { serviceCode, serviceEditionCode }, (resultSet, rowNum) -> fromResultSet(resultSet)).stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public Collection<TopicMapping> getTopicMappings() throws Exception {
        return jdbc.query("SELECT * FROM `topic_mappings`;", ((resultSet, rowNum) -> fromResultSet(resultSet)));
    }

    @Override
    public void createTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception{
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO `topic_mappings` VALUES (?, ?, ?, ?, ?);", serviceCode,
                serviceEditionCode, topic, enabled, logEntry);
    }

    @Override
    public void updateTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception {
        jdbc.update("UPDATE `topic_mappings` SET `topic`=?, `enabled`=?, `current_log_entry`=? WHERE `service_code`=? AND `service_edition_code`=?;",
                topic, enabled, logEntry, serviceCode, serviceEditionCode);
    }

    private TopicMapping fromResultSet(ResultSet resultSet) throws SQLException {
        return new TopicMapping(
                resultSet.getString("service_code"),
                resultSet.getString("service_edition_code"),
                resultSet.getString("topic"),
                resultSet.getLong("current_log_entry"),
                resultSet.getBoolean("enabled"));
    }
}