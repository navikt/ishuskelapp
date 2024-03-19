package no.nav.syfo.huskelapp.api

import no.nav.syfo.huskelapp.domain.Huskelapp
import no.nav.syfo.huskelapp.domain.Oppfolgingsgrunn
import java.time.LocalDate
import java.time.LocalDateTime

data class HuskelappRequestDTO(
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
        fun fromOppfolgingsoppgave(oppfolgingsoppgave: Huskelapp) =
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
