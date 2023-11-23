package no.nav.syfo.huskelapp.api

import java.time.LocalDate
import java.time.OffsetDateTime

data class HuskelappRequestDTO(
    val tekst: String,
    val frist: LocalDate? = null,
)

data class HuskelappResponseDTO(
    val uuid: String,
    val createdBy: String,
    val updatedAt: OffsetDateTime,
    val tekst: String,
    val frist: LocalDate?,
)
