package no.nav.syfo.api.model

import no.nav.syfo.domain.OppfolgingsoppgaveNew
import no.nav.syfo.domain.PersonIdent
import java.time.LocalDateTime

data class OppfolgingsoppgaveNewResponseDTO(
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
        fun fromOppfolgingsoppgaveNew(oppfolgingsoppgaveNew: OppfolgingsoppgaveNew) =
            OppfolgingsoppgaveNewResponseDTO(
                uuid = oppfolgingsoppgaveNew.uuid.toString(),
                personIdent = oppfolgingsoppgaveNew.personIdent,
                createdAt = oppfolgingsoppgaveNew.createdAt.toLocalDateTime(),
                updatedAt = oppfolgingsoppgaveNew.updatedAt.toLocalDateTime(),
                isActive = oppfolgingsoppgaveNew.isActive,
                publishedAt = oppfolgingsoppgaveNew.publishedAt?.toLocalDateTime(),
                removedBy = oppfolgingsoppgaveNew.removedBy,
                versjoner = oppfolgingsoppgaveNew.versjoner.map {
                    OppfolgingoppgaveVersjonResponseDTO.fromOppfolgingsoppgaveVersjon(
                        it
                    )
                },
            )
    }
}
