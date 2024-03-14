package no.nav.syfo.huskelapp.api

import java.time.LocalDate

data class EditedOppfolgingsoppgaveDTO(
    val tekst: String?,
    val frist: LocalDate,
)
