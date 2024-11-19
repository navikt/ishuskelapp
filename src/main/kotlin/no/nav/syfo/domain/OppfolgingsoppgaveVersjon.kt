package no.nav.syfo.domain

import no.nav.syfo.util.nowUTC
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class OppfolgingsoppgaveVersjon private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val createdBy: String,
    val tekst: String?,
    val oppfolgingsgrunn: Oppfolgingsgrunn,
    val frist: LocalDate?,
) {
    fun edit(
        tekst: String?,
        frist: LocalDate?,
        createdAt: OffsetDateTime = nowUTC(),
        veilederIdent: String
    ): OppfolgingsoppgaveVersjon {
        if (this.tekst == tekst && this.frist == frist) {
            throw IllegalArgumentException("No changes detected, not updating oppfolgingsoppgave")
        }

        return this.copy(
            uuid = UUID.randomUUID(),
            createdAt = createdAt,
            createdBy = veilederIdent,
            tekst = tekst,
            frist = frist,
        )
    }

    companion object {
        fun create(
            veilederIdent: String,
            tekst: String?,
            oppfolgingsgrunn: Oppfolgingsgrunn,
            frist: LocalDate? = null,
            createdAt: OffsetDateTime = nowUTC()
        ): OppfolgingsoppgaveVersjon {
            return OppfolgingsoppgaveVersjon(
                uuid = UUID.randomUUID(),
                createdAt = createdAt,
                createdBy = veilederIdent,
                tekst = tekst,
                oppfolgingsgrunn = oppfolgingsgrunn,
                frist = frist,
            )
        }

        fun createFromDatabase(
            uuid: UUID,
            createdAt: OffsetDateTime,
            createdBy: String,
            tekst: String?,
            oppfolgingsgrunn: String,
            frist: LocalDate?,
        ) = OppfolgingsoppgaveVersjon(
            uuid = uuid,
            createdAt = createdAt,
            createdBy = createdBy,
            tekst = tekst,
            oppfolgingsgrunn = Oppfolgingsgrunn.valueOf(oppfolgingsgrunn),
            frist = frist,
        )
    }
}
