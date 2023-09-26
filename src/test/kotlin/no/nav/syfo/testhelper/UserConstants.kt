package no.nav.syfo.testhelper

import no.nav.syfo.domain.PersonIdent

object UserConstants {

    private const val ARBEIDSTAKER_FNR = "12345678912"

    val ARBEIDSTAKER_PERSONIDENT = PersonIdent(ARBEIDSTAKER_FNR)
    val PERSONIDENT_VEILEDER_NO_ACCESS = PersonIdent(ARBEIDSTAKER_PERSONIDENT.value.replace("3", "1"))

    const val VEILEDER_IDENT = "Z999999"
    const val OTHER_VEILEDER_IDENT = "Z999998"
}
