package no.nav.syfo.huskelapp

import no.nav.syfo.application.metric.COUNT_HUSKELAPP_CREATED
import no.nav.syfo.application.metric.COUNT_HUSKELAPP_REMOVED
import no.nav.syfo.application.metric.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.database.HuskelappRepository
import no.nav.syfo.huskelapp.database.PHuskelapp
import no.nav.syfo.huskelapp.domain.Huskelapp
import java.time.LocalDate
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
        tekst: String,
        frist: LocalDate?,
    ) {
        val huskelapp = huskelappRepository.getHuskelapper(personIdent).firstOrNull()

        if (huskelapp?.isActive == true) {
            val huskelappVersjon = huskelappRepository.getHuskelappVersjoner(huskelapp.id).first()
            if (tekst != huskelappVersjon.tekst) {
                huskelappRepository.createVersjon(
                    huskelappId = huskelapp.id,
                    veilederIdent = veilederIdent,
                    tekst = tekst,
                    frist = frist,
                )
                COUNT_HUSKELAPP_VERSJON_CREATED.increment()
            }
        } else {
            huskelappRepository.create(
                huskelapp = Huskelapp.create(
                    tekst = tekst,
                    frist = frist,
                    personIdent = personIdent,
                    veilederIdent = veilederIdent,
                )
            )
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
