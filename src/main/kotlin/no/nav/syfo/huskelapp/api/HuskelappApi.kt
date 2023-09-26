package no.nav.syfo.huskelapp.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.VeilederTilgangskontrollPlugin
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.getNAVIdent
import no.nav.syfo.util.getPersonIdent

const val huskelappApiBasePath = "/api/internad/v1/huskelapp"

private const val API_ACTION = "access huskelapp for person"

fun Route.registerHuskelappApi(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
) {
    route(huskelappApiBasePath) {
        install(VeilederTilgangskontrollPlugin) {
            this.action = API_ACTION
            this.veilederTilgangskontrollClient = veilederTilgangskontrollClient
        }
        get {
            val personIdent = call.personIdent()

            call.respond(HttpStatusCode.OK)
        }

        post {
            val personIdent = call.personIdent()
            val requestDTO = call.receive<HuskelappRequestDTO>()
            val veilederIdent = call.getNAVIdent()

            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun ApplicationCall.personIdent(): PersonIdent = this.getPersonIdent()
    ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
