package no.nav.syfo.huskelapp.database

import java.time.OffsetDateTime
import java.util.*

data class PHuskelappVersjon(
    val id: Int,
    val uuid: UUID,
    val huskelappId: Int,
    val createdAt: OffsetDateTime,
    val createdBy: String,
    val tekst: String,
    val oppfolgingsgrunner: List<String>,
)
