package no.nav.syfo.application.metric

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

const val METRICS_NS = "ishuskelapp"

val METRICS_REGISTRY = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

const val HUSKELAPP_CREATED =
    "${METRICS_NS}_huskelapp_created_count"
val COUNT_HUSKELAPP_CREATED: Counter =
    Counter.builder(HUSKELAPP_CREATED)
        .description("Counts the number of huskelapp created")
        .register(METRICS_REGISTRY)

const val HUSKELAPP_VERSJON_CREATED =
    "${METRICS_NS}_huskelapp_versjon_created_count"
val COUNT_HUSKELAPP_VERSJON_CREATED: Counter =
    Counter.builder(HUSKELAPP_VERSJON_CREATED)
        .description("Counts the number of huskelapp versjon created")
        .register(METRICS_REGISTRY)

const val HUSKELAPP_REMOVED =
    "${METRICS_NS}_huskelapp_removed_count"
val COUNT_HUSKELAPP_REMOVED: Counter =
    Counter.builder(HUSKELAPP_REMOVED)
        .description("Counts the number of huskelapp removed")
        .register(METRICS_REGISTRY)
