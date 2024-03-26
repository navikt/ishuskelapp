package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.domain.Huskelapp
import java.time.OffsetDateTime
import java.util.*

data class PHuskelapp(
    val id: Int,
    val uuid: UUID,
    val personIdent: PersonIdent,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val isActive: Boolean,
    val publishedAt: OffsetDateTime?,
    val removedBy: String?,
) {
    fun toHuskelapp(pHuskelappVersjon: PHuskelappVersjon): Huskelapp {
        return Huskelapp.createFromDatabase(
            uuid = uuid,
            personIdent = personIdent,
            veilederIdent = pHuskelappVersjon.createdBy,
            tekst = pHuskelappVersjon.tekst,
            oppfolgingsgrunner = pHuskelappVersjon.oppfolgingsgrunner,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = pHuskelappVersjon.createdAt,
            publishedAt = publishedAt,
            removedBy = removedBy,
            frist = pHuskelappVersjon.frist,
        )
    }
}
