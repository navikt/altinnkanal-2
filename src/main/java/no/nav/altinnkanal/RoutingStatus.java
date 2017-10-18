package no.nav.altinnkanal;

public enum RoutingStatus {
    FAILED_DISABLED,
    FAILED_MISSING,
    FAILED_ERROR,
    SUCCESS;

    private String influxName;
    RoutingStatus() {
        this.influxName = name().toLowerCase();
    }

    public String getInfluxName() {
        return influxName;
    }
}
