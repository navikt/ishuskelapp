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
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT_NO_WRITE_ACCESS
import no.nav.syfo.util.JWT_CLAIM_NAVIDENT
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

private val navIdentsWithNoWriteTilgang = setOf(VEILEDER_IDENT_NO_WRITE_ACCESS)

private fun HttpRequestData.navIdent(): String? =
    headers[HttpHeaders.Authorization]
        ?.removePrefix("Bearer ")
        ?.let { token ->
            runCatching { JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString() }.getOrNull()
        }

fun MockRequestHandleScope.isTilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    val requestUrl = request.url.encodedPath
    return when {
        requestUrl.endsWith(TILGANGSKONTROLL_PERSON_PATH) -> {
            val erGodkjent = request.headers[NAV_PERSONIDENT_HEADER] != PERSONIDENT_VEILEDER_NO_ACCESS.value
            val fullTilgang = request.navIdent() !in navIdentsWithNoWriteTilgang
            respond(Tilgang(erGodkjent = erGodkjent, fullTilgang = fullTilgang))
        }

        requestUrl.endsWith(TILGANGSKONTROLL_BRUKERE_PATH) ->
            when (request.navIdent()) {
                VEILEDER_IDENT_NO_WRITE_ACCESS -> respond(listOf(ARBEIDSTAKER_FNR, ARBEIDSTAKER_2_FNR, ARBEIDSTAKER_3_FNR))
                OTHER_VEILEDER_IDENT -> respond(listOf(ARBEIDSTAKER_FNR, ARBEIDSTAKER_2_FNR))
                FAILS_VEILEDER_IDENT -> respond(emptyList<String>())
                else -> respond(listOf(ARBEIDSTAKER_FNR, ARBEIDSTAKER_2_FNR, ARBEIDSTAKER_3_FNR))
            }

        else -> respond(Tilgang(erGodkjent = false))
    }
}
