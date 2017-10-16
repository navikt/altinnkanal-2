package no.nav.altinnkanal.entities;

import java.time.LocalDateTime;

public class TopicMapping {
    private String serviceCode;
    private String serviceEditionCode;
    private String topic;
    private Boolean enabled;

    public TopicMapping() {}

    public TopicMapping(String serviceCode, String serviceEditionCode, String topic, Boolean enabled) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
        this.topic = topic;
        this.enabled = enabled;
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
}
