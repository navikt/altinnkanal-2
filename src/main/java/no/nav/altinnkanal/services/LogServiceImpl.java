package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.LogEvent;
import no.nav.altinnkanal.entities.LogEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class LogServiceImpl implements LogService {
    JdbcTemplate jdbcTemplate;

    @Autowired
    public LogServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void logChange(LogEvent logEvent) {
        jdbcTemplate.update("INSERT INTO `topic_mapping_log` VALUES(?, ?, ?, ?, ?, ?, ?)",
                logEvent.getServiceCode(), logEvent.getServiceEditionCode(), logEvent.getOldTopic(),
                logEvent.getNewTopic(), logEvent.getLogEventType().name(), Timestamp.valueOf(logEvent.getUpdateDate()),
                logEvent.getUpdatedBy());
    }

    @Override
    public List<LogEvent> getChangelog() {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping_log`;", (resultSet, rowCount) -> fromResultSet(resultSet));
    }

    @Override
    public List<LogEvent> getChangeLogFor(String serviceCode, String serviceEditionCode) throws SQLException {
        return jdbcTemplate.query("SELECT * FROM `topic_mapping_log` WHERE `service_code`=? AND `service_edition_code`=?;",
                new String[] { serviceCode, serviceEditionCode }, (resultSet, rowCount) -> fromResultSet(resultSet));
    }

    private LogEvent fromResultSet(ResultSet resultSet) throws SQLException {
        return new LogEvent(
                resultSet.getString("service_code"),
                resultSet.getString("service_edition_code"),
                resultSet.getString("old_topic"),
                resultSet.getString("new_topic"),
                LogEventType.valueOf(resultSet.getString("log_event_type")),
                resultSet.getTimestamp("updated_date").toLocalDateTime(),
                resultSet.getString("updated_by")
        );
    }
}
