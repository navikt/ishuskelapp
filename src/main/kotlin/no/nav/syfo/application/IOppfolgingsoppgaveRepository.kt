import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.OppfolgingsoppgaveNew
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgaveVersjon
import java.util.*

interface IOppfolgingsoppgaveRepository {
    fun getPOppfolgingsoppgaver(personIdent: PersonIdent): List<POppfolgingsoppgave>
    fun getOppfolgingsoppgaverNew(personIdent: PersonIdent): List<OppfolgingsoppgaveNew>
    fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Pair<POppfolgingsoppgave, POppfolgingsoppgaveVersjon>>
    fun getActiveOppfolgingsoppgaverNew(personidenter: List<PersonIdent>): List<OppfolgingsoppgaveNew>
    fun getPOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave?
    fun getOppfolgingsoppgaveNew(uuid: UUID): OppfolgingsoppgaveNew?
    fun getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon>
    fun create(oppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave
    fun create(oppfolgingsoppgaveNew: OppfolgingsoppgaveNew): OppfolgingsoppgaveNew
    fun edit(existingOppfolgingsoppgave: OppfolgingsoppgaveNew): OppfolgingsoppgaveNew?
    fun remove(oppfolgingsoppgaveNew: OppfolgingsoppgaveNew)
    fun createVersion(
        oppfolgingsoppgaveId: Int,
        newOppfolgingsoppgaveVersion: Oppfolgingsoppgave,
    ): POppfolgingsoppgaveVersjon

    fun getUnpublished(): List<POppfolgingsoppgave>
    fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave)
    fun updateRemovedOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave)
}
