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
import java.util.*

const val huskelappApiBasePath = "/api/internad/v1/huskelapp"
const val huskelappParam = "huskelappUuid"

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
                    uuid = huskelapp.uuid.toString(),
                    createdBy = huskelapp.createdBy,
                    updatedAt = huskelapp.updatedAt.toLocalDateTime(),
                    createdAt = huskelapp.createdAt.toLocalDateTime(),
                    tekst = huskelapp.tekst,
                    oppfolgingsgrunn = huskelapp.oppfolgingsgrunner.firstOrNull(),
                    frist = huskelapp.frist,
                )

                call.respond(responseDTO)
            }
        }
        post {
            val personIdent = call.personIdent()
            val requestDTO = call.receive<HuskelappRequestDTO>()
            val veilederIdent = call.getNAVIdent()

            if (requestDTO.tekst != null) {
                huskelappService.createHuskelappDeprecated(
                    personIdent = personIdent,
                    veilederIdent = veilederIdent,
                    tekst = requestDTO.tekst,
                    frist = requestDTO.frist,
                )
                call.respond(HttpStatusCode.Created)
            } else if (requestDTO.oppfolgingsgrunn != null) {
                huskelappService.createHuskelapp(
                    personIdent = personIdent,
                    veilederIdent = veilederIdent,
                    oppfolgingsgrunn = requestDTO.oppfolgingsgrunn,
                    frist = requestDTO.frist,
                )
                call.respond(HttpStatusCode.Created)
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Invalid request. Either specify `tekst` or specify `oppfolgingsgrunn`"
                )
            }
        }
        delete("/{$huskelappParam}") {
            val huskelappUuid = UUID.fromString(call.parameters[huskelappParam])
            val veilederIdent = call.getNAVIdent()

            val huskelapp = huskelappService.getHuskelapp(uuid = huskelappUuid)
            if (huskelapp == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                huskelappService.removeHuskelapp(huskelapp = huskelapp, veilederIdent = veilederIdent)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ApplicationCall.personIdent(): PersonIdent = this.getPersonIdent()
    ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
