package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TopicServiceImpl implements TopicService {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TopicServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping` WHERE `service_code`=? AND `service_edition_code`=?;",
              new String[] { serviceCode, serviceEditionCode }, this::fromResultSet);
    }

    @Override
    public TopicMapping createTopicMapping(String serviceCode, String serviceEditionCode, String topic, Boolean enabled, String user, String comment) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO `topic_mapping` VALUES (?, ?, ?, ?, ?, ?, ?);", serviceCode,
                serviceEditionCode, topic, enabled, user, now, comment);
        return new TopicMapping(serviceCode, serviceEditionCode, topic, enabled, user, now, comment);
    }

    @Override
    public List<TopicMapping> getTopicMappings() {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping`;", (rs, rowNum) -> fromResultSet(rs));
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
