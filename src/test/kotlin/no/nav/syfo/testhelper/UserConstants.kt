package no.nav.syfo.testhelper

import no.nav.syfo.domain.PersonIdent

object UserConstants {

    const val ARBEIDSTAKER_FNR = "12345678912"
    const val ARBEIDSTAKER_2_FNR = "12345678922"
    const val ARBEIDSTAKER_3_FNR = "12345678933"

    val ARBEIDSTAKER_PERSONIDENT = PersonIdent(ARBEIDSTAKER_FNR)
    val ARBEIDSTAKER_2_PERSONIDENT = PersonIdent(ARBEIDSTAKER_2_FNR)
    val ARBEIDSTAKER_3_PERSONIDENT = PersonIdent(ARBEIDSTAKER_3_FNR)
    val PERSONIDENT_VEILEDER_NO_ACCESS = PersonIdent(ARBEIDSTAKER_PERSONIDENT.value.replace("3", "1"))

    const val VEILEDER_IDENT = "Z999999"
    const val OTHER_VEILEDER_IDENT = "Z999998"
    const val FAILS_VEILEDER_IDENT = "Z999997"
}
