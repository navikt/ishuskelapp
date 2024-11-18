package no.nav.syfo.api.model

import no.nav.syfo.domain.OppfolgingsoppgaveHistorikk
import no.nav.syfo.domain.PersonIdent
import java.time.LocalDateTime

data class OppfolgingsoppgaveHistorikkResponseDTO(
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
        fun fromOppfolgingsoppgaveHistorikk(oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk) =
            OppfolgingsoppgaveHistorikkResponseDTO(
                uuid = oppfolgingsoppgaveHistorikk.uuid.toString(),
                personIdent = oppfolgingsoppgaveHistorikk.personIdent,
                createdAt = oppfolgingsoppgaveHistorikk.createdAt.toLocalDateTime(),
                updatedAt = oppfolgingsoppgaveHistorikk.updatedAt.toLocalDateTime(),
                isActive = oppfolgingsoppgaveHistorikk.isActive,
                publishedAt = oppfolgingsoppgaveHistorikk.publishedAt?.toLocalDateTime(),
                removedBy = oppfolgingsoppgaveHistorikk.removedBy,
                versjoner = oppfolgingsoppgaveHistorikk.versjoner.map {
                    OppfolgingoppgaveVersjonResponseDTO.fromOppfolgingsoppgaveVersjon(
                        it
                    )
                },
            )
    }
}
