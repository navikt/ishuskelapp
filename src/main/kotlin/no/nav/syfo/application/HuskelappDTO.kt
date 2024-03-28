package no.nav.syfo.application

import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.Oppfolgingsgrunn
import java.time.LocalDate
import java.time.LocalDateTime

data class OppfolgingsoppgaveRequestDTO(
    val tekst: String?,
    val oppfolgingsgrunn: Oppfolgingsgrunn,
    val frist: LocalDate? = null,
)

data class HuskelappResponseDTO(
    val uuid: String,
    val createdBy: String,
    val updatedAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val tekst: String?,
    val oppfolgingsgrunn: Oppfolgingsgrunn,
    val frist: LocalDate?,
) {
    companion object {
        fun fromOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave) =
            HuskelappResponseDTO(
                uuid = oppfolgingsoppgave.uuid.toString(),
                createdBy = oppfolgingsoppgave.createdBy,
                updatedAt = oppfolgingsoppgave.updatedAt.toLocalDateTime(),
                createdAt = oppfolgingsoppgave.createdAt.toLocalDateTime(),
                tekst = oppfolgingsoppgave.tekst,
                oppfolgingsgrunn = oppfolgingsoppgave.oppfolgingsgrunner.first(),
                frist = oppfolgingsoppgave.frist,
            )
    }
}
