package no.nav.altinnkanal.entities;

public class TopicMapping {
    private String serviceCode;
    private String serviceEditionCode;
    private String topic;
    private long logEntry;
    private Boolean enabled;

    public TopicMapping() {}

    public TopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
        this.topic = topic;
        this.logEntry = logEntry;
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

    public long getLogEntry() {
        return logEntry;
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

    public void setLogEntry(long logEntry) {
        this.logEntry = logEntry;
    }

    @Override
    public String toString() {
        return "TopicMapping{" +
                "serviceCode='" + serviceCode + '\'' +
                ", serviceEditionCode='" + serviceEditionCode + '\'' +
                ", topic='" + topic + '\'' +
                ", logEntry=" + logEntry +
                ", enabled=" + enabled +
                '}';
    }
}
