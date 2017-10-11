package no.nav.altinnkanal.entities;

import java.time.LocalDateTime;

public class LogEvent {
    private String serviceCode;
    private String serviceEditionCode;
    private String oldTopic;
    private String newTopic;
    private LogEventType logEventType;
    private LocalDateTime updateDate;
    private String updatedBy;

    public LogEvent() {
    }

    public LogEvent(String serviceCode, String serviceEditionCode, String oldTopic, String newTopic, LogEventType logEventType, LocalDateTime updateDate, String updatedBy) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
        this.oldTopic = oldTopic;
        this.newTopic = newTopic;
        this.logEventType = logEventType;
        this.updateDate = updateDate;
        this.updatedBy = updatedBy;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getServiceEditionCode() {
        return serviceEditionCode;
    }

    public String getOldTopic() {
        return oldTopic;
    }

    public String getNewTopic() {
        return newTopic;
    }

    public LogEventType getLogEventType() {
        return logEventType;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public void setServiceEditionCode(String serviceEditionCode) {
        this.serviceEditionCode = serviceEditionCode;
    }

    public void setOldTopic(String oldTopic) {
        this.oldTopic = oldTopic;
    }

    public void setNewTopic(String newTopic) {
        this.newTopic = newTopic;
    }

    public void setLogEventType(LogEventType logEventType) {
        this.logEventType = logEventType;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
