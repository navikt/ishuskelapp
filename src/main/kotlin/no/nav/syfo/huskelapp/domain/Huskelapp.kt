package no.nav.syfo.huskelapp.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.nowUTC
import java.time.OffsetDateTime
import java.util.UUID

data class Huskelapp private constructor(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val veilederIdent: String,
    val tekst: String,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
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
                veilederIdent = veilederIdent,
                tekst = tekst,
                isActive = true,
                createdAt = now,
                updatedAt = now,
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
        ) = Huskelapp(
            uuid = uuid,
            personIdent = personIdent,
            veilederIdent = veilederIdent,
            tekst = tekst,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
