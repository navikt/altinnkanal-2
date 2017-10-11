package no.nav.altinnkanal.entities;

import java.time.LocalDateTime;

public class TopicMapping {
    private String serviceCode;
    private String serviceEditionCode;
    private String topic;
    private Boolean enabled;
    private String lastChangedBy;
    private LocalDateTime lastChangedDate;
    private String comment;

    public TopicMapping() {}

    public TopicMapping(String serviceCode, String serviceEditionCode, String topic, Boolean enabled, String lastChangedBy,
                        LocalDateTime lastChangedDate, String comment) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
        this.topic = topic;
        this.enabled = enabled;
        this.lastChangedBy = lastChangedBy;
        this.lastChangedDate = lastChangedDate;
        this.comment = comment;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getServiceEditionCode() {
        return serviceEditionCode;
    }

    public String getTopic() {
        return topic;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public String getLastChangedBy() {
        return lastChangedBy;
    }

    public LocalDateTime getLastChangedDate() {
        return lastChangedDate;
    }

    public String getComment() {
        return comment;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public void setServiceEditionCode(String serviceEditionCode) {
        this.serviceEditionCode = serviceEditionCode;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setLastChangedBy(String lastChangedBy) {
        this.lastChangedBy = lastChangedBy;
    }

    public void setLastChangedDate(LocalDateTime lastChangedDate) {
        this.lastChangedDate = lastChangedDate;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
