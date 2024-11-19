import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.OppfolgingsoppgaveHistorikk
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgaveVersjon
import java.util.*

interface IOppfolgingsoppgaveRepository {
    fun getOppfolgingsoppgaver(personIdent: PersonIdent): List<POppfolgingsoppgave>
    fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Pair<POppfolgingsoppgave, POppfolgingsoppgaveVersjon>>
    fun getOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave?
    fun getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon>
    fun create(oppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave
    fun create(oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk): OppfolgingsoppgaveHistorikk
    fun edit(oppfolgingsoppgaveId: Int, oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk): OppfolgingsoppgaveHistorikk
    fun remove(oppfolgingsoppgave: OppfolgingsoppgaveHistorikk)
    fun createVersion(
        oppfolgingsoppgaveId: Int,
        newOppfolgingsoppgaveVersion: Oppfolgingsoppgave,
    ): POppfolgingsoppgaveVersjon

    fun getUnpublished(): List<POppfolgingsoppgave>
    fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave)
    fun updateRemovedOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave)
}
