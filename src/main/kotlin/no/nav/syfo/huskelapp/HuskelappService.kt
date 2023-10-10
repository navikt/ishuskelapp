package no.nav.syfo.huskelapp

import no.nav.syfo.domain.PersonIdent
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
        tekst: String,
    ) {
        val huskelapp = huskelappRepository.getHuskelapper(personIdent).firstOrNull()

        if (huskelapp?.isActive == true) {
            val huskelappVersjon = huskelappRepository.getHuskelappVersjoner(huskelapp.id).first()
            if (!tekst.equals(huskelappVersjon.tekst)) {
                huskelappRepository.createVersjon(
                    huskelappId = huskelapp.id,
                    veilederIdent = veilederIdent,
                    tekst = tekst,
                )
            }
        } else {
            huskelappRepository.create(
                huskelapp = Huskelapp.create(
                    tekst = tekst,
                    personIdent = personIdent,
                    veilederIdent = veilederIdent,
                )
            )
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
    }

    private fun PHuskelapp.toHuskelapp(): Huskelapp {
        val huskelappVersjon = huskelappRepository.getHuskelappVersjoner(this.id).first()
        return this.toHuskelapp(huskelappVersjon)
    }
}
