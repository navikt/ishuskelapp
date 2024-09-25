package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Oppfolgingsoppgave private constructor(
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

    fun edit(tekst: String?, frist: LocalDate?, veilederIdent: String): Oppfolgingsoppgave {
        if (this.tekst == tekst && this.frist == frist) {
            throw IllegalArgumentException("No changes detected, not updating oppfolgingsoppgave")
        }
        return this.copy(
            tekst = tekst,
            frist = frist,
            updatedAt = nowUTC(),
            createdBy = veilederIdent,
            publishedAt = null,
        )
    }

    fun publish(): Oppfolgingsoppgave {
        val now = nowUTC()
        return this.copy(updatedAt = now, publishedAt = now)
    }

    fun remove(veilederIdent: String): Oppfolgingsoppgave {
        return this.copy(
            isActive = false,
            publishedAt = null,
            updatedAt = nowUTC(),
            removedBy = veilederIdent,
        )
    }

    companion object {
        fun create(
            personIdent: PersonIdent,
            veilederIdent: String,
            tekst: String?,
            oppfolgingsgrunner: List<Oppfolgingsgrunn>,
            frist: LocalDate? = null,
        ): Oppfolgingsoppgave {
            val now = nowUTC()
            return Oppfolgingsoppgave(
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
        ) = Oppfolgingsoppgave(
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
}
