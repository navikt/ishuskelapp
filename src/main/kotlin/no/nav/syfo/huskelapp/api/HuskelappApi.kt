package no.nav.syfo.huskelapp.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.VeilederTilgangskontrollPlugin
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.getNAVIdent
import no.nav.syfo.util.getPersonIdent

const val huskelappApiBasePath = "/api/internad/v1/huskelapp"

private const val API_ACTION = "access huskelapp for person"

fun Route.registerHuskelappApi(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    huskelappService: HuskelappService,
) {
    route(huskelappApiBasePath) {
        install(VeilederTilgangskontrollPlugin) {
            this.action = API_ACTION
            this.veilederTilgangskontrollClient = veilederTilgangskontrollClient
        }
        get {
            val personIdent = call.personIdent()

            val huskelapp = huskelappService.getHuskelapp(personIdent)

            if (huskelapp == null) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                val responseDTO = HuskelappResponseDTO(
                    veilederIdent = huskelapp.veilederIdent,
                    updatedAt = huskelapp.updatedAt,
                    tekst = huskelapp.tekst,
                )

                call.respond(responseDTO)
            }
        }

        post {
            val personIdent = call.personIdent()
            val requestDTO = call.receive<HuskelappRequestDTO>()
            val veilederIdent = call.getNAVIdent()

            huskelappService.createHuskelapp(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                tekst = requestDTO.tekst,
            )

            call.respond(HttpStatusCode.Created)
        }
    }
}

private fun ApplicationCall.personIdent(): PersonIdent = this.getPersonIdent()
    ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")