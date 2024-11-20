import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.OppfolgingsoppgaveHistorikk
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgaveVersjon
import java.time.LocalDate
import java.util.*

interface IOppfolgingsoppgaveRepository {
    fun getPOppfolgingsoppgaver(personIdent: PersonIdent): List<POppfolgingsoppgave>
    fun getOppfolgingsoppgaverHistorikk(personIdent: PersonIdent): List<OppfolgingsoppgaveHistorikk>
    fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Pair<POppfolgingsoppgave, POppfolgingsoppgaveVersjon>>
    fun getPOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave?
    fun getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon>
    fun create(oppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave
    fun create(oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk): OppfolgingsoppgaveHistorikk
    fun edit(existingOppfolgingsoppgaveUuid: UUID,
             veilederIdent: String,
             newTekst: String?,
             newFrist: LocalDate?): OppfolgingsoppgaveHistorikk?
    fun remove(oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk)
    fun createVersion(
        oppfolgingsoppgaveId: Int,
        newOppfolgingsoppgaveVersion: Oppfolgingsoppgave,
    ): POppfolgingsoppgaveVersjon

    fun getUnpublished(): List<POppfolgingsoppgave>
    fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave)
    fun updateRemovedOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave)
}
