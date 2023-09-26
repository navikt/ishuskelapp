package no.nav.syfo.huskelapp

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.database.HuskelappRepository
import no.nav.syfo.huskelapp.domain.Huskelapp

class HuskelappService(
    val huskelappRepository: HuskelappRepository
) {
    fun getHuskelapp(personIdent: PersonIdent): Huskelapp? {
        val huskelapp = huskelappRepository.getHuskelapper(personIdent).firstOrNull()
        return huskelapp?.let {
            val huskelappVersjon = huskelappRepository.getHuskelappVersjoner(it.id).first()
            it.toHuskelapp(huskelappVersjon)
        }
    }

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
}
