package no.nav.syfo.huskelapp

import no.nav.syfo.application.metric.COUNT_HUSKELAPP_CREATED
import no.nav.syfo.application.metric.COUNT_HUSKELAPP_REMOVED
import no.nav.syfo.application.metric.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.api.HuskelappRequestDTO
import no.nav.syfo.huskelapp.database.HuskelappRepository
import no.nav.syfo.huskelapp.database.PHuskelapp
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
        huskelapp: HuskelappRequestDTO,
    ) {
        val existingHuskelapp = huskelappRepository.getHuskelapper(personIdent).firstOrNull()

        if (existingHuskelapp?.isActive == true) {
            val huskelappVersjon = huskelappRepository.getHuskelappVersjoner(existingHuskelapp.id).first()
            if (!huskelapp.oppfolgingsgrunner.equals(huskelappVersjon.oppfolgingsgrunner)) {
                huskelappRepository.createVersjon(
                    huskelappId = existingHuskelapp.id,
                    veilederIdent = veilederIdent,
                    oppfolgingsgrunn = huskelapp.oppfolgingsgrunner,
                )
                COUNT_HUSKELAPP_VERSJON_CREATED.increment()
            }
        } else {
            val newHuskelapp = Huskelapp.create(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                tekst = "", // Litt usikker på hvordan man skal håndtere at man ikke lenger sender tekst på en pen måte
                oppfolgingsgrunner = huskelapp.oppfolgingsgrunner
            )
            huskelappRepository.create(huskelapp = newHuskelapp)
            COUNT_HUSKELAPP_CREATED.increment()
            COUNT_HUSKELAPP_VERSJON_CREATED.increment()
        }
    }

    fun getUnpublishedHuskelapper(): List<Huskelapp> = huskelappRepository.getUnpublished().map { it.toHuskelapp() }

    fun setPublished(huskelapp: Huskelapp) = huskelappRepository.setPublished(huskelapp = huskelapp)

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
