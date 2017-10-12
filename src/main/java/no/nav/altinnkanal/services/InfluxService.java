package no.nav.altinnkanal.services;

public interface InfluxService {
    void logFailedDisabled(String serviceCode, String serviceEditionCode);
    void logFailedMissing(String serviceCode, String serviceEditionCode);
    void logSuccessful(String serviceCode, String serviceEditionCode);
}
