package no.nav.syfo.huskelapp.api

import java.time.LocalDate
import java.time.LocalDateTime

data class HuskelappRequestDTO(
    val tekst: String?,
    val oppfolgingsgrunn: String?,
    val frist: LocalDate? = null,
)

data class HuskelappResponseDTO(
    val uuid: String,
    val createdBy: String,
    val updatedAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val tekst: String?,
    val oppfolgingsgrunn: String?,
    val frist: LocalDate?,
)
