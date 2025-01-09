package no.nav.syfo.api.model

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.PersonIdent
import java.time.LocalDate
import java.time.LocalDateTime

data class OppfolgingsoppgaveRequestDTO(
    val tekst: String?,
    val oppfolgingsgrunn: Oppfolgingsgrunn,
    val frist: LocalDate? = null,
)

data class OppfolgingsoppgaveResponseDTO(
    val uuid: String,
    val personIdent: PersonIdent,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val publishedAt: LocalDateTime?,
    val removedBy: String?,
    val versjoner: List<OppfolgingoppgaveVersjonResponseDTO>
) {
    companion object {
        fun fromOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave) =
            OppfolgingsoppgaveResponseDTO(
                uuid = oppfolgingsoppgave.uuid.toString(),
                personIdent = oppfolgingsoppgave.personIdent,
                createdAt = oppfolgingsoppgave.createdAt.toLocalDateTime(),
                updatedAt = oppfolgingsoppgave.updatedAt.toLocalDateTime(),
                isActive = oppfolgingsoppgave.isActive,
                publishedAt = oppfolgingsoppgave.publishedAt?.toLocalDateTime(),
                removedBy = oppfolgingsoppgave.removedBy,
                versjoner = oppfolgingsoppgave.versjoner.map {
                    OppfolgingoppgaveVersjonResponseDTO.fromOppfolgingsoppgaveVersjon(
                        it
                    )
                },
            )
    }
}
