package no.nav.syfo.huskelapp.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.nowUTC
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Huskelapp private constructor(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val createdBy: String,
    val tekst: String?,
    val oppfolgingsgrunner: List<Oppfolgingsgrunn>,
    val frist: LocalDate?,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val publishedAt: OffsetDateTime?,
    val removedBy: String?,
) {

    fun edit(frist: LocalDate): Huskelapp =
        this.copy(frist = frist, updatedAt = nowUTC(), publishedAt = null)

    fun publish(): Huskelapp {
        val now = nowUTC()
        return this.copy(updatedAt = now, publishedAt = now)
    }

    companion object {
        fun create(
            personIdent: PersonIdent,
            veilederIdent: String,
            tekst: String?,
            oppfolgingsgrunner: List<Oppfolgingsgrunn>,
            frist: LocalDate? = null,
        ): Huskelapp {
            val now = nowUTC()
            return Huskelapp(
                uuid = UUID.randomUUID(),
                personIdent = personIdent,
                createdBy = veilederIdent,
                tekst = tekst,
                oppfolgingsgrunner = oppfolgingsgrunner,
                frist = frist,
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
            tekst: String?,
            oppfolgingsgrunner: List<String>,
            frist: LocalDate?,
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
            oppfolgingsgrunner = oppfolgingsgrunner.map { Oppfolgingsgrunn.valueOf(it) },
            frist = frist,
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
