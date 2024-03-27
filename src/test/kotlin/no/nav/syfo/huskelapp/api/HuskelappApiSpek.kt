package no.nav.syfo.huskelapp.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.application.OppfolgingsoppgaveRequestDTO
import no.nav.syfo.application.HuskelappResponseDTO
import no.nav.syfo.api.endpoints.huskelappApiBasePath
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.testhelper.*
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.util.*

class HuskelappApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe(HuskelappApiSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )
            val huskelappRepository = OppfolgingsoppgaveRepository(
                database = database,
            )

            afterEachTest {
                database.dropData()
            }

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = UserConstants.VEILEDER_IDENT,
            )

            describe("Get huskelapp for person") {
                val huskelapp = Oppfolgingsoppgave.create(
                    personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = UserConstants.VEILEDER_IDENT,
                    tekst = "En huskelapp",
                    oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
                )
                val inactiveHuskelapp = huskelapp.copy(
                    uuid = UUID.randomUUID(),
                    isActive = false
                )

                describe("Happy path") {
                    it("Returns OK if active huskelapp exists") {
                        huskelappRepository.create(oppfolgingsoppgave = huskelapp)

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                        }
                    }
                    it("Returns no content if no huskelapp exists") {
                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NoContent
                        }
                    }
                    it("Returns no content if no active huskelapp exists") {
                        huskelappRepository.create(oppfolgingsoppgave = inactiveHuskelapp)

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NoContent
                        }
                    }

                    describe("Unhappy path") {
                        it("Returns status Unauthorized if no token is supplied") {
                            testMissingToken(huskelappApiBasePath, HttpMethod.Get)
                        }
                        it("returns status Forbidden if denied access to person") {
                            testDeniedPersonAccess(huskelappApiBasePath, validToken, HttpMethod.Get)
                        }
                        it("returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                            testMissingPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Get)
                        }
                        it("returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                            testInvalidPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Get)
                        }
                    }
                }
            }
            describe("Post huskelapp") {
                describe("Happy path") {
                    val requestDTO = OppfolgingsoppgaveRequestDTO(
                        tekst = "En tekst",
                        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
                        frist = LocalDate.now().plusDays(1),
                    )
                    it("OK with tekst") {
                        val requestDTOWithTekst = OppfolgingsoppgaveRequestDTO(
                            tekst = "En tekst",
                            oppfolgingsgrunn = Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT,
                        )
                        with(
                            handleRequest(HttpMethod.Post, huskelappApiBasePath) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTOWithTekst))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val responseDTO =
                                objectMapper.readValue<HuskelappResponseDTO>(response.content!!)
                            responseDTO.tekst shouldBeEqualTo requestDTOWithTekst.tekst
                            responseDTO.oppfolgingsgrunn shouldBeEqualTo requestDTOWithTekst.oppfolgingsgrunn
                            responseDTO.createdBy shouldBeEqualTo UserConstants.VEILEDER_IDENT
                        }
                    }
                    it("OK with oppfolgingsgrunn") {
                        val requestDTOWithOppfolgingsgrunn = OppfolgingsoppgaveRequestDTO(
                            oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
                            tekst = null,
                            frist = null,
                        )
                        with(
                            handleRequest(HttpMethod.Post, huskelappApiBasePath) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTOWithOppfolgingsgrunn))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val responseDTO =
                                objectMapper.readValue<HuskelappResponseDTO>(response.content!!)

                            responseDTO.tekst shouldBeEqualTo requestDTOWithOppfolgingsgrunn.tekst
                            responseDTO.oppfolgingsgrunn shouldBeEqualTo requestDTOWithOppfolgingsgrunn.oppfolgingsgrunn
                            responseDTO.frist shouldBeEqualTo requestDTOWithOppfolgingsgrunn.frist
                            responseDTO.createdBy shouldBeEqualTo UserConstants.VEILEDER_IDENT
                        }
                    }
                    it("Does not store unchanged") {
                        with(
                            handleRequest(HttpMethod.Post, huskelappApiBasePath) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }
                        with(
                            handleRequest(HttpMethod.Post, huskelappApiBasePath) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }
                        val huskelapp =
                            huskelappRepository.getOppfolgingsoppgaver(UserConstants.ARBEIDSTAKER_PERSONIDENT).first()
                        huskelappRepository.getOppfolgingsoppgaveVersjoner(huskelapp.id).size shouldBeEqualTo 1
                    }
                }
                describe("Unhappy path") {
                    it("Returns status Unauthorized if no token is supplied") {
                        testMissingToken(huskelappApiBasePath, HttpMethod.Post)
                    }
                    it("returns status Forbidden if denied access to person") {
                        testDeniedPersonAccess(huskelappApiBasePath, validToken, HttpMethod.Post)
                    }
                    it("returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                        testMissingPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Post)
                    }
                    it("returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                        testInvalidPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Post)
                    }
                }
            }
            describe("Post new version of oppfolgingsoppgave") {
                describe("Happy path") {
                    it("OK with new date") {
                        val huskelapp = Oppfolgingsoppgave.create(
                            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                            veilederIdent = UserConstants.VEILEDER_IDENT,
                            tekst = "En huskelapp",
                            oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE),
                            frist = LocalDate.now().minusDays(1)
                        )
                        val existingOppfolgingsoppgave = huskelappRepository.create(oppfolgingsoppgave = huskelapp)
                        val requestDTO = OppfolgingsoppgaveRequestDTO(
                            tekst = existingOppfolgingsoppgave.tekst,
                            oppfolgingsgrunn = existingOppfolgingsoppgave.oppfolgingsgrunner.first(),
                            frist = LocalDate.now().plusDays(1),
                        )

                        with(
                            handleRequest(HttpMethod.Post, "$huskelappApiBasePath/${existingOppfolgingsoppgave.uuid}") {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val responseDTO =
                                objectMapper.readValue<HuskelappResponseDTO>(response.content!!)

                            responseDTO.tekst shouldBeEqualTo requestDTO.tekst
                            responseDTO.oppfolgingsgrunn shouldBeEqualTo requestDTO.oppfolgingsgrunn
                            responseDTO.frist shouldBeEqualTo requestDTO.frist
                            responseDTO.createdBy shouldBeEqualTo UserConstants.VEILEDER_IDENT
                        }
                    }
                }
                describe("Unhappy path") {
                    it("Returns status NotFound if oppfolgingsoppgave does not exist") {
                        val requestDTO = OppfolgingsoppgaveRequestDTO(
                            tekst = null,
                            oppfolgingsgrunn = Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT,
                            frist = LocalDate.now().plusDays(1),
                        )
                        with(
                            handleRequest(HttpMethod.Post, "$huskelappApiBasePath/${UUID.randomUUID()}") {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NotFound
                        }
                    }
                }
            }
            describe("Delete huskelapp") {
                val huskelapp = Oppfolgingsoppgave.create(
                    personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = UserConstants.VEILEDER_IDENT,
                    tekst = "En huskelapp",
                    oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE),
                )
                val inactiveHuskelapp = huskelapp.copy(
                    uuid = UUID.randomUUID(),
                    isActive = false
                )

                val deleteHuskelappUrl = "$huskelappApiBasePath/${huskelapp.uuid}"

                describe("Happy path") {
                    it("returns NoContent and sets huskelapp inactive") {
                        huskelappRepository.create(oppfolgingsoppgave = huskelapp)

                        with(
                            handleRequest(HttpMethod.Delete, deleteHuskelappUrl) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NoContent
                        }

                        val huskelapper =
                            huskelappRepository.getOppfolgingsoppgaver(UserConstants.ARBEIDSTAKER_PERSONIDENT)
                        huskelapper.size shouldBeEqualTo 1
                        huskelapper.first().isActive.shouldBeFalse()
                    }
                }
                describe("Unhappy path") {
                    it("returns status NotFound if no huskelapp exists for given uuid") {
                        with(
                            handleRequest(HttpMethod.Delete, "$huskelappApiBasePath/${UUID.randomUUID()}") {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NotFound
                        }
                    }
                    it("returns status NotFound if no active huskelapp exists for given uuid") {
                        huskelappRepository.create(oppfolgingsoppgave = inactiveHuskelapp)

                        with(
                            handleRequest(HttpMethod.Delete, "$huskelappApiBasePath/${inactiveHuskelapp.uuid}") {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NotFound
                        }
                    }
                    it("returns status Unauthorized if no token is supplied") {
                        testMissingToken(deleteHuskelappUrl, HttpMethod.Delete)
                    }
                    it("returns status Forbidden if denied access to person") {
                        testDeniedPersonAccess(deleteHuskelappUrl, validToken, HttpMethod.Delete)
                    }
                    it("returns status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                        testMissingPersonIdent(deleteHuskelappUrl, validToken, HttpMethod.Delete)
                    }
                    it("returns status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                        testInvalidPersonIdent(deleteHuskelappUrl, validToken, HttpMethod.Delete)
                    }
                }
            }
        }
    }
})

private fun TestApplicationEngine.testMissingToken(url: String, httpMethod: HttpMethod) {
    with(
        handleRequest(httpMethod, url) {}
    ) {
        response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
    }
}

private fun TestApplicationEngine.testMissingPersonIdent(
    url: String,
    validToken: String,
    httpMethod: HttpMethod,
) {
    with(
        handleRequest(httpMethod, url) {
            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
        }
    ) {
        response.status() shouldBeEqualTo HttpStatusCode.BadRequest
    }
}

private fun TestApplicationEngine.testInvalidPersonIdent(
    url: String,
    validToken: String,
    httpMethod: HttpMethod,
) {
    with(
        handleRequest(httpMethod, url) {
            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
            addHeader(
                NAV_PERSONIDENT_HEADER,
                UserConstants.ARBEIDSTAKER_PERSONIDENT.value.drop(1)
            )
        }
    ) {
        response.status() shouldBeEqualTo HttpStatusCode.BadRequest
    }
}

private fun TestApplicationEngine.testDeniedPersonAccess(
    url: String,
    validToken: String,
    httpMethod: HttpMethod,
) {
    with(
        handleRequest(httpMethod, url) {
            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
            addHeader(
                NAV_PERSONIDENT_HEADER,
                UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS.value
            )
        }
    ) {
        response.status() shouldBeEqualTo HttpStatusCode.Forbidden
    }
}
