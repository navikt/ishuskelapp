package no.nav.syfo.api.model

import no.nav.syfo.domain.Oppfolgingsgrunn
import java.time.LocalDate

data class EditedOppfolgingsoppgaveDTO(
    val oppfolgingsgrunn: Oppfolgingsgrunn?,
    val tekst: String?,
    val frist: LocalDate?,
)
