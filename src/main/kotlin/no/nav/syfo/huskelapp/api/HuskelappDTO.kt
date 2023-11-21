package no.nav.syfo.huskelapp.api

import java.time.OffsetDateTime

data class HuskelappRequestDTO(
    val oppfolgingsgrunner: List<String>,
)

data class HuskelappResponseDTO(
    val uuid: String,
    val createdBy: String,
    val updatedAt: OffsetDateTime,
    val tekst: String,
)
