package no.nav.syfo.huskelapp.api

import java.time.OffsetDateTime

data class HuskelappRequestDTO(
    val tekst: String,
)

data class HuskelappResponseDTO(
    val veilederIdent: String,
    val updatedAt: OffsetDateTime,
    val tekst: String,
)
