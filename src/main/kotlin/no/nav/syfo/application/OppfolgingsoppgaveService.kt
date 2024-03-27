package no.nav.syfo.application

import IOppfolgingsoppgaveRepository
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_CREATED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_REMOVED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.repository.POppfolgingsoppgave
import no.nav.syfo.domain.Oppfolgingsoppgave
import java.util.*

class OppfolgingsoppgaveService(
    private val oppfolgingsoppgaveRepository: IOppfolgingsoppgaveRepository,
) {
    fun getOppfolgingsoppgave(personIdent: PersonIdent): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent)
            .firstOrNull()
            ?.takeIf { it.isActive }
            ?.toOppfolgingsoppgave()

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
        newVersion: EditedOppfolgingsoppgaveDTO
    ): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getOppfolgingsoppgave(existingOppfolgingsoppgaveUuid)
            ?.let { pExistingOppfolgingsoppgave ->
                val existingOppfolgingsoppgave = pExistingOppfolgingsoppgave.toOppfolgingsoppgave()
                val newOppfolgingsoppgaveVersion = existingOppfolgingsoppgave.edit(newVersion.tekst, newVersion.frist)
                oppfolgingsoppgaveRepository.createVersion(
                    oppfolgingsoppgaveId = pExistingOppfolgingsoppgave.id,
                    newOppfolgingsoppgaveVersion = newOppfolgingsoppgaveVersion,
                )
                COUNT_HUSKELAPP_VERSJON_CREATED.increment()
                return newOppfolgingsoppgaveVersion
            }

    fun getUnpublishedOppfolgingsoppgaver(): List<Oppfolgingsoppgave> =
        oppfolgingsoppgaveRepository.getUnpublished().map { it.toOppfolgingsoppgave() }

    fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave) =
        oppfolgingsoppgaveRepository.updatePublished(oppfolgingsoppgave = oppfolgingsoppgave)

    fun getOppfolgingsoppgave(uuid: UUID): Oppfolgingsoppgave? =
        oppfolgingsoppgaveRepository.getOppfolgingsoppgave(uuid)
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
        val oppfolgingsoppgaveVersjon = oppfolgingsoppgaveRepository.getOppfolgingsoppgaveVersjoner(this.id).first()
        return this.toOppfolgingsoppgave(oppfolgingsoppgaveVersjon)
    }
}
