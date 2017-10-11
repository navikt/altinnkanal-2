package no.nav.altinnkanal.entities;

import java.time.LocalDateTime;

public class TopicMapping {
    private String serviceCode;
    private String serviceEditionCode;
    private String topic;
    private String user;
    private LocalDateTime updated;
    private String comment;

    public TopicMapping() {}

    public TopicMapping(String serviceCode, String serviceEditionCode, String topic, String user, LocalDateTime updated,
                        String comment) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
        this.topic = topic;
        this.user = user;
        this.updated = updated;
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

    public String getUser() {
        return user;
    }

    public LocalDateTime getUpdated() {
        return updated;
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

    public void setUser(String user) {
        this.user = user;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
