package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Oppfolgingsoppgave private constructor(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val versjoner: List<OppfolgingsoppgaveVersjon>,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val publishedAt: OffsetDateTime?,
    val removedBy: String?,
) {

    fun edit(tekst: String?, frist: LocalDate?, veilederIdent: String): Oppfolgingsoppgave {
        val updatedAt = nowUTC()
        val versjon = versjoner.first().edit(
            createdAt = updatedAt,
            veilederIdent = veilederIdent,
            tekst = tekst,
            frist = frist,
        )

        return this.copy(
            updatedAt = updatedAt,
            publishedAt = null,
            versjoner = listOf(versjon),
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

    fun sisteVersjon() = versjoner.first()

    companion object {
        fun create(
            personIdent: PersonIdent,
            veilederIdent: String,
            tekst: String?,
            oppfolgingsgrunn: Oppfolgingsgrunn,
            frist: LocalDate? = null,
        ): Oppfolgingsoppgave {
            val now = nowUTC()
            val oppfolgingsoppgaveVersjon = OppfolgingsoppgaveVersjon.create(
                veilederIdent = veilederIdent,
                tekst = tekst,
                oppfolgingsgrunn = oppfolgingsgrunn,
                frist = frist,
                createdAt = now,
            )

            return Oppfolgingsoppgave(
                uuid = UUID.randomUUID(),
                personIdent = personIdent,
                versjoner = listOf(oppfolgingsoppgaveVersjon),
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
            versjoner: List<OppfolgingsoppgaveVersjon>,
            isActive: Boolean,
            createdAt: OffsetDateTime,
            updatedAt: OffsetDateTime,
            publishedAt: OffsetDateTime?,
            removedBy: String?,
        ) = Oppfolgingsoppgave(
            uuid = uuid,
            personIdent = personIdent,
            versjoner = versjoner,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            publishedAt = publishedAt,
            removedBy = removedBy,
        )
    }
}
