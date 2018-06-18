package no.nav.altinnkanal

import io.prometheus.client.Counter
import io.prometheus.client.Summary

private const val NAMESPACE = "altinnkanal"

object Metrics {
    @JvmStatic val requestsTotal: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("requests_total")
        .help("Total requests.")
        .register()

    @JvmStatic val requestsSuccess: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("requests_success")
        .help("Total successful requests.")
        .labelNames("sc", "sec")
        .register()

    @JvmStatic val requestsFailedMissing: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("requests_missing")
        .help("Total failed requests due to missing/unknown SC/SEC codes.")
        .register()

    @JvmStatic val requestsFailedError: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("requests_error")
        .help("Total failed requests due to error.")
        .register()

    @JvmStatic val requestSize: Summary = Summary.build()
        .namespace(NAMESPACE)
        .name("request_size_bytes_sum")
        .help("Request size in bytes.")
        .register()

    @JvmStatic val requestTime: Summary = Summary.build()
        .namespace(NAMESPACE)
        .name("request_time_ms")
        .help("Request time in milliseconds.")
        .register()
}