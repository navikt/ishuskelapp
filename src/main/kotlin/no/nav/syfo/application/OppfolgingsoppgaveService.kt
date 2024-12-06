package no.nav.syfo.application

import IOppfolgingsoppgaveRepository
import no.nav.syfo.api.model.OppfolgingsoppgaveRequestDTO
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_CREATED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_REMOVED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgave
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.OppfolgingsoppgaveNew
import java.time.LocalDate
import java.util.*

class OppfolgingsoppgaveService(
    private val oppfolgingsoppgaveRepository: IOppfolgingsoppgaveRepository,
) {
    fun getOppfolgingsoppgave(personIdent: PersonIdent): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(personIdent)
            .firstOrNull()
            ?.takeIf { it.isActive }
            ?.toOppfolgingsoppgave()

    fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Oppfolgingsoppgave> =
        oppfolgingsoppgaveRepository.getActiveOppfolgingsoppgaver(personidenter)
            .map { it.first.toOppfolgingsoppgave(pOppfolgingsoppgaveVersjon = it.second) }

    fun getActiveOppfolgingsoppgaverNew(personidenter: List<PersonIdent>): List<OppfolgingsoppgaveNew> =
        oppfolgingsoppgaveRepository.getActiveOppfolgingsoppgaverNew(personidenter)

    fun getOppfolgingsoppgaver(personIdent: PersonIdent): List<OppfolgingsoppgaveNew> =
        oppfolgingsoppgaveRepository.getOppfolgingsoppgaverNew(personIdent)

    fun createOppfolgingsoppgave(
        personIdent: PersonIdent,
        veilederIdent: String,
        newOppfolgingsoppgave: OppfolgingsoppgaveRequestDTO,
    ) {
        oppfolgingsoppgaveRepository.create(
            oppfolgingsoppgave = Oppfolgingsoppgave.create(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                tekst = newOppfolgingsoppgave.tekst,
                oppfolgingsgrunner = listOf(newOppfolgingsoppgave.oppfolgingsgrunn),
                frist = newOppfolgingsoppgave.frist,
            )
        )
        COUNT_HUSKELAPP_CREATED.increment()
        COUNT_HUSKELAPP_VERSJON_CREATED.increment()
    }

    fun addVersion(
        existingOppfolgingsoppgaveUuid: UUID,
        veilederIdent: String,
        newTekst: String?,
        newFrist: LocalDate?,
    ): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getPOppfolgingsoppgave(existingOppfolgingsoppgaveUuid)
            ?.let { pExistingOppfolgingsoppgave ->
                val existingOppfolgingsoppgave = pExistingOppfolgingsoppgave.toOppfolgingsoppgave()
                val newOppfolgingsoppgaveVersion = existingOppfolgingsoppgave.edit(
                    veilederIdent = veilederIdent,
                    tekst = newTekst,
                    frist = newFrist,
                )
                oppfolgingsoppgaveRepository.createVersion(
                    oppfolgingsoppgaveId = pExistingOppfolgingsoppgave.id,
                    newOppfolgingsoppgaveVersion = newOppfolgingsoppgaveVersion,
                )
                COUNT_HUSKELAPP_VERSJON_CREATED.increment()
                return newOppfolgingsoppgaveVersion
            }

    fun editOppfolgingsoppgave(
        existingOppfolgingsoppgaveUuid: UUID,
        veilederIdent: String,
        newTekst: String?,
        newFrist: LocalDate?,
    ): OppfolgingsoppgaveNew? {
        return oppfolgingsoppgaveRepository.getOppfolgingsoppgaveNew(existingOppfolgingsoppgaveUuid)
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
        oppfolgingsoppgaveRepository.getUnpublished().map { it.toOppfolgingsoppgave() }

    fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave) =
        oppfolgingsoppgaveRepository.updatePublished(oppfolgingsoppgave = oppfolgingsoppgave)

    fun getOppfolgingsoppgave(uuid: UUID): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getPOppfolgingsoppgave(uuid)
            ?.takeIf { it.isActive }
            ?.toOppfolgingsoppgave()

    fun removeOppfolgingsoppgave(
        oppfolgingsoppgave: Oppfolgingsoppgave,
        veilederIdent: String,
    ) {
        val removedOppfolgingsoppgave = oppfolgingsoppgave.remove(veilederIdent = veilederIdent)
        oppfolgingsoppgaveRepository.updateRemovedOppfolgingsoppgave(oppfolgingsoppgave = removedOppfolgingsoppgave)
        COUNT_HUSKELAPP_REMOVED.increment()
    }

    private fun POppfolgingsoppgave.toOppfolgingsoppgave(): Oppfolgingsoppgave {
        val latestOppfolgingsoppgaveVersjon =
            oppfolgingsoppgaveRepository.getOppfolgingsoppgaveVersjoner(this.id).first()
        return this.toOppfolgingsoppgave(latestOppfolgingsoppgaveVersjon)
    }
}
