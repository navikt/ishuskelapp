package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.api.VeilederTilgangskontrollPlugin
import no.nav.syfo.application.EditedOppfolgingsoppgaveDTO
import no.nav.syfo.application.OppfolgingsoppgaveRequestDTO
import no.nav.syfo.application.HuskelappResponseDTO
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.getNAVIdent
import no.nav.syfo.util.getPersonIdent
import java.util.*

const val huskelappApiBasePath = "/api/internad/v1/huskelapp"
const val huskelappParam = "huskelappUuid"

private const val API_ACTION = "access huskelapp for person"

fun Route.registerHuskelappApi(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    huskelappService: OppfolgingsoppgaveService,
) {
    route(huskelappApiBasePath) {
        install(VeilederTilgangskontrollPlugin) {
            this.action = API_ACTION
            this.veilederTilgangskontrollClient = veilederTilgangskontrollClient
        }
        get {
            val personIdent = call.personIdent()

            val huskelapp = huskelappService.getOppfolgingsoppgave(personIdent)

            if (huskelapp == null) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                val responseDTO = HuskelappResponseDTO.fromOppfolgingsoppgave(huskelapp)
                call.respond(responseDTO)
            }
        }
        post {
            val personIdent = call.personIdent()
            val requestDTO = call.receive<OppfolgingsoppgaveRequestDTO>()
            val veilederIdent = call.getNAVIdent()

            huskelappService.createOppfolgingsoppgave(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                newOppfolgingsoppgave = requestDTO,
            )
            call.respond(HttpStatusCode.Created)
        }
        post("/{$huskelappParam}") {
            val uuid = UUID.fromString(call.parameters[huskelappParam])
            val requestDTO = call.receive<EditedOppfolgingsoppgaveDTO>()

            huskelappService.addVersion(
                existingOppfolgingsoppgaveUuid = uuid,
                newVersion = requestDTO
            )
                ?.let { createdHuskelappVersjon ->
                    call.respond(
                        HttpStatusCode.Created,
                        HuskelappResponseDTO.fromOppfolgingsoppgave(createdHuskelappVersjon)
                    )
                }
                ?: call.respond(HttpStatusCode.NotFound, "Could not find existing oppfolgingsoppgave with uuid: $uuid")
        }
        delete("/{$huskelappParam}") {
            val huskelappUuid = UUID.fromString(call.parameters[huskelappParam])
            val veilederIdent = call.getNAVIdent()

            val huskelapp = huskelappService.getOppfolgingsoppgave(uuid = huskelappUuid)
            if (huskelapp == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                huskelappService.removeOppfolgingsoppgave(oppfolgingsoppgave = huskelapp, veilederIdent = veilederIdent)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ApplicationCall.personIdent(): PersonIdent = this.getPersonIdent()
    ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
