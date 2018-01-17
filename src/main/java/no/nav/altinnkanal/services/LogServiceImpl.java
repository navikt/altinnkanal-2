package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMappingUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class LogServiceImpl implements LogService {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public LogServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private TopicMappingUpdate getLogEntry(long id) {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping_log` WHERE `id`=?;", new Long[] { id }, (resultSet, rowCount) -> fromResultSet(resultSet)).get(0);
    }

    @Override
    public TopicMappingUpdate logChange(TopicMappingUpdate topicMappingUpdate) throws SQLException {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update((con) -> {
            PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO `topic_mapping_log`(`service_code`, `service_edition_code`, `topic`, `enabled`, `comment`, `updated_date`, `updated_by`) VALUES(?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, topicMappingUpdate.getServiceCode());
            preparedStatement.setString(2, topicMappingUpdate.getServiceEditionCode());
            preparedStatement.setString(3, topicMappingUpdate.getTopic());
            preparedStatement.setBoolean(4, topicMappingUpdate.isEnabled());
            preparedStatement.setString(5, topicMappingUpdate.getComment());
            preparedStatement.setTimestamp(6, Timestamp.valueOf(topicMappingUpdate.getUpdateDate()));
            preparedStatement.setString(7, topicMappingUpdate.getUpdatedBy());
            return preparedStatement;
        }, keyHolder);

        return getLogEntry(keyHolder.getKey().longValue());
    }

    @Override
    public List<TopicMappingUpdate> getUniqueChangelog(Boolean enabled) throws SQLException {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping_log` INNER JOIN `topic_mappings` `mapping` ON `current_log_entry`=`id` WHERE `mapping`.`enabled`=? ORDER BY `mapping`.`service_code` ASC, `mapping`.`service_edition_code` ASC;",
                new Object[] { enabled }, (resultSet, rowCount) -> fromResultSet(resultSet));
    }

    @Override
    public List<TopicMappingUpdate> getChangeLogFor(String serviceCode, String serviceEditionCode) throws SQLException {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping_log` WHERE `service_code`=? AND `service_edition_code`=? ORDER BY `updated_date` DESC;",
                new String[] { serviceCode, serviceEditionCode }, (resultSet, rowCount) -> fromResultSet(resultSet));
    }

    @Override
    public TopicMappingUpdate getLastChangeFor(String serviceCode, String serviceEditionCode) throws SQLException {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping_log` WHERE `service_code`=? AND `service_edition_code`=? ORDER BY `updated_date` DESC LIMIT 1;",
                new String[]{ serviceCode, serviceEditionCode }, (resultSet, rowCount) -> fromResultSet(resultSet)).get(0);
    }

    @Override
    public long getLastLogEntryId() throws SQLException {
        return jdbcTemplate.query("SELECT MAX(`id`) AS `last_log_entry_id` FROM `topic_mapping_log`;",
                (resultSet, i) -> resultSet.getLong("last_log_entry_id")).get(0);
    }

    private TopicMappingUpdate fromResultSet(ResultSet resultSet) throws SQLException {
        TopicMappingUpdate topicMappingUpdate = new TopicMappingUpdate(
                resultSet.getString("service_code"),
                resultSet.getString("service_edition_code"),
                resultSet.getString("topic"),
                resultSet.getBoolean("enabled"),
                resultSet.getString("comment"),
                resultSet.getTimestamp("updated_date").toLocalDateTime(),
                resultSet.getString("updated_by")
        );

        topicMappingUpdate.setId(resultSet.getLong("id"));

        return topicMappingUpdate;
    }
}
