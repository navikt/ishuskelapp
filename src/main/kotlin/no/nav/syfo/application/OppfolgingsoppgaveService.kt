package no.nav.syfo.application

import IOppfolgingsoppgaveRepository
import no.nav.syfo.api.model.OppfolgingsoppgaveRequestDTO
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_CREATED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_REMOVED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Oppfolgingsoppgave
import java.time.LocalDate
import java.util.*

class OppfolgingsoppgaveService(
    private val oppfolgingsoppgaveRepository: IOppfolgingsoppgaveRepository,
) {
    fun getActiveOppfolgingsoppgave(personIdent: PersonIdent): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent)
            .firstOrNull()
            ?.takeIf { it.isActive }

    fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Oppfolgingsoppgave> =
        oppfolgingsoppgaveRepository.getActiveOppfolgingsoppgaver(personidenter)

    fun getOppfolgingsoppgaver(personIdent: PersonIdent): List<Oppfolgingsoppgave> =
        oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent)

    fun createOppfolgingsoppgave(
        personIdent: PersonIdent,
        veilederIdent: String,
        newOppfolgingsoppgave: OppfolgingsoppgaveRequestDTO,
    ): Oppfolgingsoppgave {
        val oppfolgingsoppgave = oppfolgingsoppgaveRepository.create(
            oppfolgingsoppgave = Oppfolgingsoppgave.create(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                tekst = newOppfolgingsoppgave.tekst,
                oppfolgingsgrunn = newOppfolgingsoppgave.oppfolgingsgrunn,
                frist = newOppfolgingsoppgave.frist,
            )
        )
        COUNT_HUSKELAPP_CREATED.increment()
        COUNT_HUSKELAPP_VERSJON_CREATED.increment()
        return oppfolgingsoppgave
    }

    fun editOppfolgingsoppgave(
        existingOppfolgingsoppgaveUuid: UUID,
        veilederIdent: String,
        newTekst: String?,
        newFrist: LocalDate?,
    ): Oppfolgingsoppgave? {
        return oppfolgingsoppgaveRepository.getOppfolgingsoppgave(existingOppfolgingsoppgaveUuid)
            ?.let { existingOppfolgingsoppgave ->
                existingOppfolgingsoppgave
                    .edit(
                        veilederIdent = veilederIdent,
                        tekst = newTekst,
                        frist = newFrist,
                    )
                    .run { oppfolgingsoppgaveRepository.edit(this) }
            }
    }

    fun getUnpublishedOppfolgingsoppgaver(): List<Oppfolgingsoppgave> =
        oppfolgingsoppgaveRepository.getUnpublished()

    fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave) =
        oppfolgingsoppgaveRepository.updatePublished(oppfolgingsoppgave = oppfolgingsoppgave)

    fun getActiveOppfolgingsoppgave(uuid: UUID): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getOppfolgingsoppgave(uuid)
            ?.takeIf { it.isActive }

    fun removeOppfolgingsoppgave(
        oppfolgingsoppgave: Oppfolgingsoppgave,
        veilederIdent: String,
    ) {
        val removedOppfolgingsoppgave = oppfolgingsoppgave.remove(veilederIdent = veilederIdent)
        oppfolgingsoppgaveRepository.updateRemovedOppfolgingsoppgave(oppfolgingsoppgave = removedOppfolgingsoppgave)
        COUNT_HUSKELAPP_REMOVED.increment()
    }
}
