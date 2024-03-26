package no.nav.syfo.huskelapp

import no.nav.syfo.application.metric.COUNT_HUSKELAPP_CREATED
import no.nav.syfo.application.metric.COUNT_HUSKELAPP_REMOVED
import no.nav.syfo.application.metric.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.api.HuskelappRequestDTO
import no.nav.syfo.huskelapp.api.EditedOppfolgingsoppgaveDTO
import no.nav.syfo.infrastructure.database.repository.HuskelappRepository
import no.nav.syfo.infrastructure.database.repository.PHuskelapp
import no.nav.syfo.huskelapp.domain.Huskelapp
import java.util.*

class HuskelappService(
    private val huskelappRepository: HuskelappRepository,
) {
    fun getHuskelapp(personIdent: PersonIdent): Huskelapp? =
        huskelappRepository.getHuskelapper(personIdent)
            .firstOrNull()
            ?.takeIf { it.isActive }
            ?.toHuskelapp()

    fun createHuskelapp(
        personIdent: PersonIdent,
        veilederIdent: String,
        newHuskelapp: HuskelappRequestDTO,
    ) {
        huskelappRepository.create(
            huskelapp = Huskelapp.create(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                tekst = newHuskelapp.tekst,
                oppfolgingsgrunner = listOf(newHuskelapp.oppfolgingsgrunn),
                frist = newHuskelapp.frist,
            )
        )
        COUNT_HUSKELAPP_CREATED.increment()
        COUNT_HUSKELAPP_VERSJON_CREATED.increment()
    }

    fun addVersion(
        existingOppfolgingsoppgaveUuid: UUID,
        newVersion: EditedOppfolgingsoppgaveDTO
    ): Huskelapp? =
        huskelappRepository.getHuskelapp(existingOppfolgingsoppgaveUuid)
            ?.let { pExistingOppfolgingsoppgave ->
                val existingOppfolgingsoppgave = pExistingOppfolgingsoppgave.toHuskelapp()
                val newOppfolgingsoppgaveVersion = existingOppfolgingsoppgave.edit(newVersion.tekst, newVersion.frist)
                huskelappRepository.createVersion(
                    huskelappId = pExistingOppfolgingsoppgave.id,
                    newOppfolgingsoppgaveVersion = newOppfolgingsoppgaveVersion,
                )
                COUNT_HUSKELAPP_VERSJON_CREATED.increment()
                return newOppfolgingsoppgaveVersion
            }

    fun getUnpublishedHuskelapper(): List<Huskelapp> = huskelappRepository.getUnpublished().map { it.toHuskelapp() }

    fun updatePublished(huskelapp: Huskelapp) = huskelappRepository.updatePublished(huskelapp = huskelapp)

    fun getHuskelapp(uuid: UUID): Huskelapp? =
        huskelappRepository.getHuskelapp(uuid)
            ?.takeIf { it.isActive }
            ?.toHuskelapp()

    fun removeHuskelapp(
        huskelapp: Huskelapp,
        veilederIdent: String,
    ) {
        val removedHuskelapp = huskelapp.remove(veilederIdent = veilederIdent)
        huskelappRepository.updateRemovedHuskelapp(huskelapp = removedHuskelapp)
        COUNT_HUSKELAPP_REMOVED.increment()
    }

    private fun PHuskelapp.toHuskelapp(): Huskelapp {
        val huskelappVersjon = huskelappRepository.getHuskelappVersjoner(this.id).first()
        return this.toHuskelapp(huskelappVersjon)
    }
}
