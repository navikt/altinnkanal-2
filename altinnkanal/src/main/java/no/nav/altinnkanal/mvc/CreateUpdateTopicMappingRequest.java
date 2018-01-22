package no.nav.altinnkanal.mvc;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateUpdateTopicMappingRequest {
    @Size(min = 1)
    private String serviceCode;
    @Size(min = 1)
    private String serviceEditionCode;
    private String topic;
    @NotNull
    private boolean enabled;
    private String comment;

    public CreateUpdateTopicMappingRequest(String serviceCode, String serviceEditionCode, String topic, boolean enabled, String comment) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
        this.topic = topic;
        this.enabled = enabled;
        this.comment = comment;
    }

    public CreateUpdateTopicMappingRequest() {
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

    public boolean isEnabled() {
        return enabled;
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

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
