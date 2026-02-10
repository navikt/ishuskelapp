package no.nav.syfo.api.endpoints

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.api.endpoints.RequestParameters.IS_ACTIVE
import no.nav.syfo.api.model.EditedOppfolgingsoppgaveDTO
import no.nav.syfo.api.model.OppfolgingsoppgaveRequestDTO
import no.nav.syfo.api.model.OppfolgingsoppgaveResponseDTO
import no.nav.syfo.api.model.OppfolgingsoppgaverRequestDTO
import no.nav.syfo.api.model.OppfolgingsoppgaverResponseDTO
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.domain.NoChangesDetectedException
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.checkVeilederTilgang
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getNAVIdent
import no.nav.syfo.util.getPersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

const val huskelappApiBasePath = "/api/internad/v1/huskelapp"
const val huskelappParam = "huskelappUuid"

private const val API_ACTION = "access oppfolgingsoppgave for person"

val logger: Logger = LoggerFactory.getLogger("no.nav.syfo")

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
                val isActive = call.request.queryParameters[IS_ACTIVE]?.toBoolean() ?: false

                if (isActive) {
                    val oppfolgingsoppgave = oppfolgingsoppgaveService.getActiveOppfolgingsoppgave(personIdent)

                    if (oppfolgingsoppgave == null) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        val responseDTO = OppfolgingsoppgaveResponseDTO.fromOppfolgingsoppgave(oppfolgingsoppgave)
                        call.respond(responseDTO)
                    }
                } else {
                    val responseDTO = oppfolgingsoppgaveService.getOppfolgingsoppgaver(personIdent).map {
                        OppfolgingsoppgaveResponseDTO.fromOppfolgingsoppgave(it)
                    }
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

                val oppfolgingsoppgave = oppfolgingsoppgaveService.createOppfolgingsoppgave(
                    personIdent = personIdent,
                    veilederIdent = veilederIdent,
                    oppfolgingsgrunn = requestDTO.oppfolgingsgrunn,
                    tekst = requestDTO.tekst,
                    frist = requestDTO.frist,
                )
                call.respond(HttpStatusCode.Created, oppfolgingsoppgave)
            }
        }

        post("/{$huskelappParam}") {
            call.checkVeilederTilgang(
                action = API_ACTION,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val uuid = UUID.fromString(call.parameters[huskelappParam])
                val requestDTO = call.receive<EditedOppfolgingsoppgaveDTO>()
                val veilederIdent = call.getNAVIdent()

                try {
                    oppfolgingsoppgaveService.editOppfolgingsoppgave(
                        existingOppfolgingsoppgaveUuid = uuid,
                        veilederIdent = veilederIdent,
                        newOppfolgingsgrunn = requestDTO.oppfolgingsgrunn,
                        newTekst = requestDTO.tekst,
                        newFrist = requestDTO.frist,
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
                } catch (error: NoChangesDetectedException) {
                    logger.warn(error.message)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        delete("/{$huskelappParam}") {
            call.checkVeilederTilgang(
                action = API_ACTION,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val oppfolgingsoppgaveUuid = UUID.fromString(call.parameters[huskelappParam])
                val veilederIdent = call.getNAVIdent()

                val oppfolgingsoppgave =
                    oppfolgingsoppgaveService.getActiveOppfolgingsoppgave(uuid = oppfolgingsoppgaveUuid)
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

        post("/get-oppfolgingsoppgaver") {
            val token = call.getBearerHeader()
                ?: throw IllegalArgumentException("Failed to get oppfolgingsoppgaver for personer. No Authorization header supplied.")
            val requestDTOList = call.receive<OppfolgingsoppgaverRequestDTO>()

            val personerVeilederHasAccessTo = veilederTilgangskontrollClient.veilederPersonerAccess(
                personidenter = requestDTOList.personidenter.map { PersonIdent(it) },
                token = token,
                callId = call.getCallId(),
            )

            val oppfolgingsoppgaver = if (personerVeilederHasAccessTo.isNullOrEmpty()) {
                emptyList()
            } else {
                oppfolgingsoppgaveService.getActiveOppfolgingsoppgaver(
                    personidenter = personerVeilederHasAccessTo,
                )
            }

            if (oppfolgingsoppgaver.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                val responseDTO = OppfolgingsoppgaverResponseDTO(
                    oppfolgingsoppgaver = oppfolgingsoppgaver.associate {
                        it.personIdent.value to OppfolgingsoppgaveResponseDTO.fromOppfolgingsoppgave(it)
                    }
                )
                call.respond(responseDTO)
            }
        }
    }
}

private fun ApplicationCall.personIdent(): PersonIdent = this.getPersonIdent()
    ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")

object RequestParameters {
    const val IS_ACTIVE = "isActive"
}
