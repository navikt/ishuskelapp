package no.nav.syfo.infrastructure.database.repository

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class POppfolgingsoppgaveVersjon(
    val id: Int,
    val uuid: UUID,
    val oppfolgingsoppgaveId: Int,
    val createdAt: OffsetDateTime,
    val createdBy: String,
    val tekst: String?,
    val oppfolgingsgrunner: List<String>,
    val frist: LocalDate?,
)
