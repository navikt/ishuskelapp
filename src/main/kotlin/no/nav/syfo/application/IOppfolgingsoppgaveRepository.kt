import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgaveVersjon
import java.util.*

interface IOppfolgingsoppgaveRepository {
    fun getPOppfolgingsoppgaver(personIdent: PersonIdent): List<POppfolgingsoppgave>
    fun getOppfolgingsoppgaver(personIdent: PersonIdent): List<Oppfolgingsoppgave>
    fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Oppfolgingsoppgave>
    fun getPOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave?
    fun getOppfolgingsoppgave(uuid: UUID): Oppfolgingsoppgave?
    fun getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon>
    fun create(oppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave
    fun edit(existingOppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave?
    fun remove(oppfolgingsoppgave: Oppfolgingsoppgave)
    fun getUnpublished(): List<Oppfolgingsoppgave>
    fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave)
    fun updateRemovedOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave)
    fun updatePersonident(nyPersonident: PersonIdent, oppfolgingsoppgaver: List<Oppfolgingsoppgave>)
}
