package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.domain.Oppfolgingsgrunn
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class OppfolgingsoppgaveRecord(
    val uuid: UUID,
    val personIdent: String,
    val veilederIdent: String,
    val tekst: String?,
    val oppfolgingsgrunner: List<Oppfolgingsgrunn>,
    val frist: LocalDate?,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
