package no.nav.syfo.api.endpoints

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.infrastructure.METRICS_REGISTRY

const val podMetricsPath = "/internal/metrics"

fun Routing.registerMetricApi() {
    get(podMetricsPath) {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
