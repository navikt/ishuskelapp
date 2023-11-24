package no.nav.syfo.huskelapp.kafka

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class KafkaHuskelapp(
    val uuid: UUID,
    val personIdent: String,
    val veilederIdent: String,
    val tekst: String?,
    val oppfolgingsgrunner: List<String>,
    val frist: LocalDate?,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
