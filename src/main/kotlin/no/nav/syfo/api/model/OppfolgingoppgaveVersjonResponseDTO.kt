package no.nav.syfo.api.model

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.OppfolgingsoppgaveVersjon
import java.time.LocalDate
import java.time.LocalDateTime

data class OppfolgingoppgaveVersjonResponseDTO(
    val uuid: String,
    val createdBy: String,
    val createdAt: LocalDateTime,
    val tekst: String?,
    val oppfolgingsgrunn: Oppfolgingsgrunn,
    val frist: LocalDate?,
) {
    companion object {
        fun fromOppfolgingsoppgaveVersjon(oppfolgingsoppgaveVersjon: OppfolgingsoppgaveVersjon) =
            OppfolgingoppgaveVersjonResponseDTO(
                uuid = oppfolgingsoppgaveVersjon.uuid.toString(),
                createdBy = oppfolgingsoppgaveVersjon.createdBy,
                createdAt = oppfolgingsoppgaveVersjon.createdAt.toLocalDateTime(),
                tekst = oppfolgingsoppgaveVersjon.tekst,
                oppfolgingsgrunn = oppfolgingsoppgaveVersjon.oppfolgingsgrunn,
                frist = oppfolgingsoppgaveVersjon.frist,
            )
    }
}
