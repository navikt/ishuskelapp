package no.nav.syfo.huskelapp.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.nowUTC
import java.time.OffsetDateTime
import java.util.UUID

data class Huskelapp private constructor(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val createdBy: String,
    val tekst: String,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val publishedAt: OffsetDateTime?,
    val removedBy: String?,
) {
    companion object {
        fun create(
            tekst: String,
            personIdent: PersonIdent,
            veilederIdent: String,
        ): Huskelapp {
            val now = nowUTC()
            return Huskelapp(
                uuid = UUID.randomUUID(),
                personIdent = personIdent,
                createdBy = veilederIdent,
                tekst = tekst,
                isActive = true,
                createdAt = now,
                updatedAt = now,
                publishedAt = null,
                removedBy = null,
            )
        }

        fun createFromDatabase(
            uuid: UUID,
            personIdent: PersonIdent,
            veilederIdent: String,
            tekst: String,
            isActive: Boolean,
            createdAt: OffsetDateTime,
            updatedAt: OffsetDateTime,
            publishedAt: OffsetDateTime?,
            removedBy: String?,
        ) = Huskelapp(
            uuid = uuid,
            personIdent = personIdent,
            createdBy = veilederIdent,
            tekst = tekst,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            publishedAt = publishedAt,
            removedBy = removedBy,
        )
    }

    fun remove(veilederIdent: String): Huskelapp {
        return this.copy(
            isActive = false,
            publishedAt = null,
            updatedAt = nowUTC(),
            removedBy = veilederIdent,
        )
    }
}
