package no.nav.altinnkanal.entities;

import java.time.LocalDateTime;

public class TopicMappingUpdate {
    private int id;
    private String serviceCode;
    private String serviceEditionCode;
    private String topic;
    private Boolean enabled;
    private String comment;
    private LocalDateTime updateDate;
    private String updatedBy;
    

    public TopicMappingUpdate() {
    }

    public TopicMappingUpdate(String serviceCode, String serviceEditionCode, String topic, Boolean enabled, String comment, LocalDateTime updateDate, String updatedBy) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
        this.topic = topic;
        this.enabled = enabled;
        this.comment = comment;
        this.updateDate = updateDate;
        this.updatedBy = updatedBy;
    }

    public int getId() {
        return id;
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

    public String getComment() {
        return comment;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setId(int id) {
        this.id = id;
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

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
