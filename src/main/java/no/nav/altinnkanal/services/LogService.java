package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.LogEvent;

import java.sql.SQLException;
import java.util.List;

public interface LogService {
    void logChange(LogEvent logEvent) throws SQLException;
    List<LogEvent> getChangelog() throws SQLException;
    List<LogEvent> getChangeLogFor(String serviceCode, String serviceEditionCode) throws SQLException;
}
