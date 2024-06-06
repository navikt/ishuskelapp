package no.nav.syfo.testhelper.mock

import com.auth0.jwt.JWT
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.infrastructure.client.veiledertilgang.Tilgang
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient.Companion.TILGANGSKONTROLL_BRUKERE_PATH
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient.Companion.TILGANGSKONTROLL_PERSON_PATH
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_3_FNR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.FAILS_VEILEDER_IDENT
import no.nav.syfo.testhelper.UserConstants.OTHER_VEILEDER_IDENT
import no.nav.syfo.testhelper.UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT
import no.nav.syfo.util.JWT_CLAIM_NAVIDENT
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.isTilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    val requestUrl = request.url.encodedPath
    return when {
        requestUrl.endsWith(TILGANGSKONTROLL_PERSON_PATH) ->
            when (request.headers[NAV_PERSONIDENT_HEADER]) {
                PERSONIDENT_VEILEDER_NO_ACCESS.value -> respond(Tilgang(erGodkjent = false))
                else -> respond(Tilgang(erGodkjent = true))
            }

        requestUrl.endsWith(TILGANGSKONTROLL_BRUKERE_PATH) ->
            when (request.getNavIdent()) {
                VEILEDER_IDENT -> respond(listOf(ARBEIDSTAKER_FNR, ARBEIDSTAKER_2_FNR, ARBEIDSTAKER_3_FNR))
                OTHER_VEILEDER_IDENT -> respond(listOf(ARBEIDSTAKER_FNR, ARBEIDSTAKER_2_FNR))
                FAILS_VEILEDER_IDENT -> respond(emptyList<String>())
                else -> respond(emptyList<String>())
            }

        else -> respond(Tilgang(erGodkjent = false))
    }
}

private fun HttpRequestData.getNavIdent(): String {
    val token = this.getBearerHeaderWithoutPrefix()
    return JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
}

private fun HttpRequestData.getBearerHeaderWithoutPrefix(): String =
    this.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: throw Error("No Authorization header supplied")
