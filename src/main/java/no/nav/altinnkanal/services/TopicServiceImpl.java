package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class TopicServiceImpl implements TopicService {
    private Connection connection;

    @Override
    public TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception {
        // TODO: Resolve this from database
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `topic_mapping` WHERE `service_code`=? AND `service_edition_code`=?;")) {
            preparedStatement.setString(1, serviceCode);
            preparedStatement.setString(2, serviceEditionCode);
            //return fromResultSet(preparedStatement.executeQuery());
        }
        return new TopicMapping(serviceCode, serviceEditionCode, "test", true, null, null, null);
    }

    @Override
    public TopicMapping createTopicMapping(String serviceCode, String serviceEditionCode, String topic, Boolean enabled, String user, String comment) {

        return new TopicMapping(serviceCode, serviceEditionCode, topic, enabled, user, LocalDateTime.now(), comment);
    }

    @Override
    public List<TopicMapping> getTopicMappings() {
        return Collections.emptyList();
    }

    private TopicMapping fromResultSet(ResultSet resultSet) throws SQLException {
        String serviceCode = resultSet.getString("service_code");
        String serviceEditionCode = resultSet.getString("service_edition_code");
        String topic = resultSet.getString("topic");
        String user = resultSet.getString("updated_by");
        Boolean enabled = resultSet.getBoolean("enabled");
        LocalDateTime updated = resultSet.getTimestamp("updated").toLocalDateTime();
        String comment = resultSet.getString("comment");
        return new TopicMapping(serviceCode, serviceEditionCode, topic, enabled, user, updated, comment);
    }
}
