package no.nav.syfo.application

import IOppfolgingsoppgaveRepository
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_CREATED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_REMOVED
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.infrastructure.COUNT_OPPFOLGINGSOPPGAVE_OPPFOLGINGSGRUNN_EDITED
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
        oppfolgingsgrunn: Oppfolgingsgrunn,
        tekst: String?,
        frist: LocalDate?,
    ): Oppfolgingsoppgave {
        val oppfolgingsoppgave = oppfolgingsoppgaveRepository.create(
            oppfolgingsoppgave = Oppfolgingsoppgave.create(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                tekst = tekst,
                oppfolgingsgrunn = oppfolgingsgrunn,
                frist = frist,
            )
        )
        COUNT_HUSKELAPP_CREATED.increment()
        COUNT_HUSKELAPP_VERSJON_CREATED.increment()
        return oppfolgingsoppgave
    }

    fun editOppfolgingsoppgave(
        existingOppfolgingsoppgaveUuid: UUID,
        veilederIdent: String,
        newOppfolgingsgrunn: Oppfolgingsgrunn?,
        newTekst: String?,
        newFrist: LocalDate?,
    ): Oppfolgingsoppgave? {
        return oppfolgingsoppgaveRepository.getOppfolgingsoppgave(existingOppfolgingsoppgaveUuid)
            ?.let { existingOppfolgingsoppgave ->
                if (newOppfolgingsgrunn != null && shouldCreateNewOppfolgingsoppgave(newOppfolgingsgrunn, existingOppfolgingsoppgave)) {
                    removeOppfolgingsoppgave(
                        oppfolgingsoppgave = existingOppfolgingsoppgave,
                        veilederIdent = veilederIdent
                    )
                    val newOppfolgingsoppgave = createOppfolgingsoppgave(
                        personIdent = existingOppfolgingsoppgave.personIdent,
                        veilederIdent = veilederIdent,
                        oppfolgingsgrunn = newOppfolgingsgrunn,
                        tekst = newTekst,
                        frist = newFrist,
                    )
                    COUNT_OPPFOLGINGSOPPGAVE_OPPFOLGINGSGRUNN_EDITED.increment()
                    newOppfolgingsoppgave
                } else {
                    existingOppfolgingsoppgave
                        .edit(
                            veilederIdent = veilederIdent,
                            tekst = newTekst,
                            frist = newFrist,
                        )
                        .run { oppfolgingsoppgaveRepository.edit(this) }
                }
            }
    }

    private fun shouldCreateNewOppfolgingsoppgave(
        newOppfolgingsgrunn: Oppfolgingsgrunn,
        existingOppfolgingsoppgave: Oppfolgingsoppgave
    ) = existingOppfolgingsoppgave.sisteVersjon().oppfolgingsgrunn != newOppfolgingsgrunn

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
