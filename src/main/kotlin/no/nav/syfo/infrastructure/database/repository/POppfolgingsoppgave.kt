package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.PersonIdent
import java.time.OffsetDateTime
import java.util.*

data class POppfolgingsoppgave(
    val id: Int,
    val uuid: UUID,
    val personIdent: PersonIdent,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val isActive: Boolean,
    val publishedAt: OffsetDateTime?,
    val removedBy: String?,
) {
    fun toOppfolgingsoppgave(pOppfolgingsoppgaveVersjon: List<POppfolgingsoppgaveVersjon>): Oppfolgingsoppgave {
        return Oppfolgingsoppgave.createFromDatabase(
            uuid = uuid,
            personIdent = personIdent,
            versjoner = pOppfolgingsoppgaveVersjon.map { it.toOppfolgingsoppgaveVersjon() },
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            publishedAt = publishedAt,
            removedBy = removedBy,
        )
    }
}
