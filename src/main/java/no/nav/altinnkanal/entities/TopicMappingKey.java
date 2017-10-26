package no.nav.altinnkanal.entities;

public class TopicMappingKey {
    private String serviceCode;
    private String serviceEditionCode;

    public TopicMappingKey(String serviceCode, String serviceEditionCode) {
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getServiceEditionCode() {
        return serviceEditionCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public void setServiceEditionCode(String serviceEditionCode) {
        this.serviceEditionCode = serviceEditionCode;
    }

    public static TopicMappingKey fromMapping(TopicMapping topicMapping) {
        return new TopicMappingKey(topicMapping.getServiceCode(), topicMapping.getServiceEditionCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;

        if (getClass() == o.getClass()) {
            TopicMappingKey other = (TopicMappingKey) o;
            return serviceCode.equals(other.serviceCode) && serviceEditionCode.equals(other.serviceEditionCode);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = serviceCode != null ? serviceCode.hashCode() : 0;
        result = 31 * result + (serviceEditionCode != null ? serviceEditionCode.hashCode() : 0);
        return result;
    }
}
