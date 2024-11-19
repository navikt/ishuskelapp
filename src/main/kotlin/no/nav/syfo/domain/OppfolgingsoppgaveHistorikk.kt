package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class OppfolgingsoppgaveHistorikk private constructor(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val versjoner: List<OppfolgingsoppgaveVersjon>,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val publishedAt: OffsetDateTime?,
    val removedBy: String?,
) {

    fun edit(tekst: String?, frist: LocalDate? = null, veilederIdent: String): OppfolgingsoppgaveHistorikk {
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

    fun publish(): OppfolgingsoppgaveHistorikk {
        val now = nowUTC()
        return this.copy(updatedAt = now, publishedAt = now)
    }

    fun remove(veilederIdent: String): OppfolgingsoppgaveHistorikk {
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
            oppfolgingsgrunn: Oppfolgingsgrunn,
            frist: LocalDate? = null,
        ): OppfolgingsoppgaveHistorikk {
            val now = nowUTC()
            val oppfolgingsoppgaveVersjon = OppfolgingsoppgaveVersjon.create(
                veilederIdent = veilederIdent,
                tekst = tekst,
                oppfolgingsgrunn = oppfolgingsgrunn,
                frist = frist,
                createdAt = now,
            )

            return OppfolgingsoppgaveHistorikk(
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
        ) = OppfolgingsoppgaveHistorikk(
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
