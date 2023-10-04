package no.nav.syfo.huskelapp

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.database.HuskelappRepository
import no.nav.syfo.huskelapp.database.PHuskelapp
import no.nav.syfo.huskelapp.domain.Huskelapp

class HuskelappService(
    private val huskelappRepository: HuskelappRepository,
) {
    fun getHuskelapp(personIdent: PersonIdent): Huskelapp? =
        huskelappRepository.getHuskelapper(personIdent).firstOrNull()?.toHuskelapp()

    fun createHuskelapp(
        personIdent: PersonIdent,
        veilederIdent: String,
        tekst: String,
    ) {
        val huskelapp = huskelappRepository.getHuskelapper(personIdent).firstOrNull()

        if (huskelapp?.isActive == true) {
            huskelappRepository.createVersjon(
                huskelappId = huskelapp.id,
                veilederIdent = veilederIdent,
                tekst = tekst,
            )
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

    private fun PHuskelapp.toHuskelapp(): Huskelapp {
        val huskelappVersjon = huskelappRepository.getHuskelappVersjoner(this.id).first()
        return this.toHuskelapp(huskelappVersjon)
    }
}
