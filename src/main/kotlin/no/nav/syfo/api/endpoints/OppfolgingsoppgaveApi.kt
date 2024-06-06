package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.api.model.*
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.util.*
import java.util.*

const val huskelappApiBasePath = "/api/internad/v1/huskelapp"
const val huskelappParam = "huskelappUuid"

private const val API_ACTION = "access oppfolgingsoppgave for person"

fun Route.registerOppfolgingsoppgaveApi(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    oppfolgingsoppgaveService: OppfolgingsoppgaveService,
) {
    route(huskelappApiBasePath) {
        
        get {
            call.checkVeilederTilgang(
                action = API_ACTION,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val personIdent = call.personIdent()
                
                val oppfolgingsoppgave = oppfolgingsoppgaveService.getOppfolgingsoppgave(personIdent)
                
                if (oppfolgingsoppgave == null) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    val responseDTO = OppfolgingsoppgaveResponseDTO.fromOppfolgingsoppgave(oppfolgingsoppgave)
                    call.respond(responseDTO)
                }
            }
        }
        
        post {
            call.checkVeilederTilgang(
                action = API_ACTION,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val personIdent = call.personIdent()
                val veilederIdent = call.getNAVIdent()
                
                val requestDTO = call.receive<OppfolgingsoppgaveRequestDTO>()
                
                oppfolgingsoppgaveService.createOppfolgingsoppgave(
                    personIdent = personIdent,
                    veilederIdent = veilederIdent,
                    newOppfolgingsoppgave = requestDTO,
                )
                call.respond(HttpStatusCode.Created)
            }
        }
        
        post("/{$huskelappParam}") {
            call.checkVeilederTilgang(
                action = API_ACTION,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val uuid = UUID.fromString(call.parameters[huskelappParam])
                val requestDTO = call.receive<EditedOppfolgingsoppgaveDTO>()
                
                oppfolgingsoppgaveService.addVersion(
                    existingOppfolgingsoppgaveUuid = uuid,
                    newVersion = requestDTO
                )
                    ?.let { createdOppfolgingsoppgaveVersjon ->
                        call.respond(
                            HttpStatusCode.Created,
                            OppfolgingsoppgaveResponseDTO.fromOppfolgingsoppgave(createdOppfolgingsoppgaveVersjon)
                        )
                    }
                    ?: call.respond(
                        HttpStatusCode.NotFound,
                        "Could not find existing oppfolgingsoppgave with uuid: $uuid"
                    )
            }
        }
        
        delete("/{$huskelappParam}") {
            call.checkVeilederTilgang(
                action = API_ACTION,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val oppfolgingsoppgaveUuid = UUID.fromString(call.parameters[huskelappParam])
                val veilederIdent = call.getNAVIdent()
                
                val oppfolgingsoppgave = oppfolgingsoppgaveService.getOppfolgingsoppgave(uuid = oppfolgingsoppgaveUuid)
                if (oppfolgingsoppgave == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    oppfolgingsoppgaveService.removeOppfolgingsoppgave(
                        oppfolgingsoppgave = oppfolgingsoppgave,
                        veilederIdent = veilederIdent
                    )
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private fun ApplicationCall.personIdent(): PersonIdent = this.getPersonIdent()
    ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")
