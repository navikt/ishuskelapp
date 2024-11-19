package no.nav.syfo.huskelapp.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.api.endpoints.FilterRequestParameter.ALL
import no.nav.syfo.api.endpoints.RequestParameters.FILTER
import no.nav.syfo.api.endpoints.huskelappApiBasePath
import no.nav.syfo.api.model.*
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.domain.*
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_2_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_3_FNR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_3_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.FAILS_VEILEDER_IDENT
import no.nav.syfo.testhelper.UserConstants.OTHER_VEILEDER_IDENT
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.util.*

class OppfolgingsoppgaveApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe(OppfolgingsoppgaveApiSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )
            val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(
                database = database,
            )
            val oppfolgingsoppgaveService = OppfolgingsoppgaveService(
                oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
            )

            afterEachTest {
                database.dropData()
            }

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = VEILEDER_IDENT,
            )
            val validTokenOtherVeileder = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = OTHER_VEILEDER_IDENT,
            )

            describe("Get oppfolgingsoppgave for person") {
                val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                    personIdent = ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = VEILEDER_IDENT,
                    tekst = "En oppfolgingsoppgave",
                    oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
                )
                val inactiveOppfolgingsoppgave = oppfolgingsoppgave.copy(
                    uuid = UUID.randomUUID(),
                    isActive = false
                )

                describe("Happy path") {
                    it("Returns OK if active oppfolgingsoppgave exists") {
                        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                        }
                    }
                    it("Returns no content if no oppfolgingsoppgave exists") {
                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NoContent
                        }
                    }
                    it("Returns no content if no active oppfolgingsoppgave exists") {
                        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = inactiveOppfolgingsoppgave)

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
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

            describe("Get oppfølgingsoppgaver med versjoner") {
                val huskelappUrlAll = "$huskelappApiBasePath?$FILTER=${ALL.value}"

                fun createOppfolgingsoppgave(tekst: String = "En oppfolgingsoppgave"): OppfolgingsoppgaveHistorikk =
                    OppfolgingsoppgaveHistorikk.create(
                        personIdent = ARBEIDSTAKER_PERSONIDENT,
                        veilederIdent = VEILEDER_IDENT,
                        tekst = tekst,
                        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
                    )

                val oppfolgingsoppgaveHistorikk = createOppfolgingsoppgave()

                it("Oppretting av oppfølgingsoppgave skal ha 1 versjon") {
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgaveHistorikk)

                    with(
                        handleRequest(HttpMethod.Get, huskelappUrlAll) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val responseDTO =
                            objectMapper.readValue<List<OppfolgingsoppgaveHistorikkResponseDTO>>(response.content!!)

                        responseDTO.size shouldBeEqualTo 1

                        val oppfolgingsoppgave = responseDTO.first()
                        val sisteVersjon = oppfolgingsoppgave.versjoner.first()

                        oppfolgingsoppgave.personIdent shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT
                        oppfolgingsoppgave.isActive shouldBe true
                        oppfolgingsoppgave.createdAt shouldBeEqualTo oppfolgingsoppgave.updatedAt
                        oppfolgingsoppgave.versjoner.size shouldBeEqualTo 1

                        sisteVersjon.createdAt shouldBeEqualTo oppfolgingsoppgave.updatedAt
                        sisteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                        sisteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        sisteVersjon.tekst shouldBeEqualTo "En oppfolgingsoppgave"
                    }
                }

                it("Oppretting av 2 oppfølgingsoppgaver skal ha 1 versjon hver") {
                    val oppfolgingsoppgave = createOppfolgingsoppgave("Oppfølgingsoppgave nr. 1")
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
                    oppfolgingsoppgaveRepository.remove(oppfolgingsoppgave.remove(VEILEDER_IDENT))
                    oppfolgingsoppgaveRepository.create(createOppfolgingsoppgave("Oppfølgingsoppgave nr. 2"))

                    with(
                        handleRequest(HttpMethod.Get, huskelappUrlAll) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val responseDTO =
                            objectMapper.readValue<List<OppfolgingsoppgaveHistorikkResponseDTO>>(response.content!!)

                        responseDTO.size shouldBeEqualTo 2

                        val sisteOppfolgingsoppgave = responseDTO.get(0)
                        val sisteVersjon = sisteOppfolgingsoppgave.versjoner.first()

                        sisteOppfolgingsoppgave.personIdent shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT
                        sisteOppfolgingsoppgave.isActive shouldBe true
                        sisteOppfolgingsoppgave.createdAt shouldBeEqualTo sisteOppfolgingsoppgave.updatedAt
                        sisteOppfolgingsoppgave.versjoner.size shouldBeEqualTo 1

                        sisteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                        sisteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        sisteVersjon.tekst shouldBeEqualTo "Oppfølgingsoppgave nr. 2"

                        val forsteOppfolgingsoppgave = responseDTO.get(1)
                        val forsteVersjon = forsteOppfolgingsoppgave.versjoner.first()

                        forsteOppfolgingsoppgave.personIdent shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT
                        forsteOppfolgingsoppgave.isActive shouldBe false
                        forsteOppfolgingsoppgave.versjoner.size shouldBeEqualTo 1

                        forsteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                        forsteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        forsteVersjon.tekst shouldBeEqualTo "Oppfølgingsoppgave nr. 1"
                    }
                }

                it("Oppretting av flere oppfølgingsoppgaver") {
                    val initiellOppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgaveHistorikk)
                    val oppdatertOppfolgingsoppgave = oppfolgingsoppgaveHistorikk.edit(
                        tekst = "En oppfolgingsoppgave oppdatert",
                        veilederIdent = VEILEDER_IDENT,
                    )
                    val pExistingOppgave = oppfolgingsoppgaveRepository.getOppfolgingsoppgave(initiellOppgave.uuid)
                    oppfolgingsoppgaveRepository.edit(pExistingOppgave!!.id, oppdatertOppfolgingsoppgave)

                    with(
                        handleRequest(HttpMethod.Get, huskelappUrlAll) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val responseDTO =
                            objectMapper.readValue<List<OppfolgingsoppgaveHistorikkResponseDTO>>(response.content!!)

                        responseDTO.size shouldBeEqualTo 1

                        val oppfolgingsoppgave = responseDTO.first()
                        val forsteVersjon = oppfolgingsoppgave.versjoner.get(1)
                        val sisteVersjon = oppfolgingsoppgave.versjoner.get(0)

                        forsteVersjon.createdAt shouldBeEqualTo oppfolgingsoppgave.createdAt
                        forsteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                        forsteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        forsteVersjon.tekst shouldBeEqualTo "En oppfolgingsoppgave"

                        sisteVersjon.createdAt shouldBeEqualTo oppfolgingsoppgave.updatedAt
                        sisteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                        sisteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        sisteVersjon.tekst shouldBeEqualTo "En oppfolgingsoppgave oppdatert"

                        oppfolgingsoppgave.personIdent shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT
                        oppfolgingsoppgave.isActive shouldBe true
                        oppfolgingsoppgave.versjoner.size shouldBeEqualTo 2
                        oppfolgingsoppgave.createdAt shouldBeEqualTo forsteVersjon.createdAt
                        oppfolgingsoppgave.updatedAt shouldBeEqualTo sisteVersjon.createdAt
                    }
                }
            }

            describe("Post oppfolgingsoppgave") {
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
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTOWithTekst))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val responseDTO =
                                objectMapper.readValue<OppfolgingsoppgaveResponseDTO>(response.content!!)
                            responseDTO.tekst shouldBeEqualTo requestDTOWithTekst.tekst
                            responseDTO.oppfolgingsgrunn shouldBeEqualTo requestDTOWithTekst.oppfolgingsgrunn
                            responseDTO.createdBy shouldBeEqualTo VEILEDER_IDENT
                        }
                    }
                    it("OK with oppfolgingsgrunn") {
                        val requestDTOWithOppfolgingsgrunn = OppfolgingsoppgaveRequestDTO(
                            oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_ANNEN_YTELSE,
                            tekst = null,
                            frist = null,
                        )
                        with(
                            handleRequest(HttpMethod.Post, huskelappApiBasePath) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTOWithOppfolgingsgrunn))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val responseDTO =
                                objectMapper.readValue<OppfolgingsoppgaveResponseDTO>(response.content!!)

                            responseDTO.tekst shouldBeEqualTo requestDTOWithOppfolgingsgrunn.tekst
                            responseDTO.oppfolgingsgrunn shouldBeEqualTo requestDTOWithOppfolgingsgrunn.oppfolgingsgrunn
                            responseDTO.frist shouldBeEqualTo requestDTOWithOppfolgingsgrunn.frist
                            responseDTO.createdBy shouldBeEqualTo VEILEDER_IDENT
                        }
                    }
                    it("Does not store unchanged") {
                        with(
                            handleRequest(HttpMethod.Post, huskelappApiBasePath) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }
                        with(
                            handleRequest(HttpMethod.Post, huskelappApiBasePath) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }
                        val oppfolgingsoppgave =
                            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(ARBEIDSTAKER_PERSONIDENT)
                                .first()
                        oppfolgingsoppgaveRepository.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgave.id).size shouldBeEqualTo 1
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
                    it("OK with new date and other veileder") {
                        val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                            personIdent = ARBEIDSTAKER_PERSONIDENT,
                            veilederIdent = VEILEDER_IDENT,
                            tekst = "En oppfolgingsoppgave",
                            oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE),
                            frist = LocalDate.now().minusDays(1)
                        )
                        val existingOppfolgingsoppgave =
                            oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                        val requestDTO = EditedOppfolgingsoppgaveDTO(
                            tekst = existingOppfolgingsoppgave.tekst,
                            frist = LocalDate.now().plusDays(1),
                        )

                        with(
                            handleRequest(HttpMethod.Post, "$huskelappApiBasePath/${existingOppfolgingsoppgave.uuid}") {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validTokenOtherVeileder))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val responseDTO =
                                objectMapper.readValue<OppfolgingsoppgaveResponseDTO>(response.content!!)

                            responseDTO.tekst shouldBeEqualTo requestDTO.tekst
                            responseDTO.frist shouldBeEqualTo requestDTO.frist
                            responseDTO.createdBy shouldBeEqualTo OTHER_VEILEDER_IDENT
                        }
                    }
                    it("OK with new tekst and same veileder") {
                        val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                            personIdent = ARBEIDSTAKER_PERSONIDENT,
                            veilederIdent = VEILEDER_IDENT,
                            tekst = "En oppfolgingsoppgave",
                            oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE),
                            frist = LocalDate.now().minusDays(1)
                        )
                        val existingOppfolgingsoppgave =
                            oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                        val requestDTO = EditedOppfolgingsoppgaveDTO(
                            tekst = "Ny tekst",
                            frist = existingOppfolgingsoppgave.frist,
                        )

                        with(
                            handleRequest(HttpMethod.Post, "$huskelappApiBasePath/${existingOppfolgingsoppgave.uuid}") {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Created
                        }

                        with(
                            handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val responseDTO =
                                objectMapper.readValue<OppfolgingsoppgaveResponseDTO>(response.content!!)

                            responseDTO.tekst shouldBeEqualTo requestDTO.tekst
                            responseDTO.frist shouldBeEqualTo requestDTO.frist
                            responseDTO.createdBy shouldBeEqualTo oppfolgingsoppgave.createdBy
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
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                                setBody(objectMapper.writeValueAsString(requestDTO))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NotFound
                        }
                    }
                }
            }
            describe("Delete oppfolgingsoppgave") {
                val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                    personIdent = ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = VEILEDER_IDENT,
                    tekst = "En oppfolgingsoppgave",
                    oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE),
                )
                val inactiveOppfolgingsoppgave = oppfolgingsoppgave.copy(
                    uuid = UUID.randomUUID(),
                    isActive = false
                )

                val deleteHuskelappUrl = "$huskelappApiBasePath/${oppfolgingsoppgave.uuid}"

                describe("Happy path") {
                    it("returns NoContent and sets oppfolgingsoppgave inactive") {
                        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)

                        with(
                            handleRequest(HttpMethod.Delete, deleteHuskelappUrl) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NoContent
                        }

                        val oppfolgingsoppgaver =
                            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(ARBEIDSTAKER_PERSONIDENT)
                        oppfolgingsoppgaver.size shouldBeEqualTo 1
                        oppfolgingsoppgaver.first().isActive.shouldBeFalse()
                    }
                }
                describe("Unhappy path") {
                    it("returns status NotFound if no oppfolgingsoppgave exists for given uuid") {
                        with(
                            handleRequest(HttpMethod.Delete, "$huskelappApiBasePath/${UUID.randomUUID()}") {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NotFound
                        }
                    }
                    it("returns status NotFound if no active oppfolgingsoppgave exists for given uuid") {
                        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = inactiveOppfolgingsoppgave)

                        with(
                            handleRequest(
                                HttpMethod.Delete,
                                "$huskelappApiBasePath/${inactiveOppfolgingsoppgave.uuid}"
                            ) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
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

            describe("POST: Get oppfolgingsoppgaver for several persons") {
                val personidenter = listOf(ARBEIDSTAKER_PERSONIDENT, ARBEIDSTAKER_2_PERSONIDENT, ARBEIDSTAKER_3_PERSONIDENT)
                val requestDTO = OppfolgingsoppgaverRequestDTO(personidenter.map { it.value })
                val url = "$huskelappApiBasePath/get-oppfolgingsoppgaver"

                fun createOppfolgingsoppgaver(identer: List<PersonIdent> = personidenter): List<Oppfolgingsoppgave> {
                    return identer.map { personident ->
                        val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                            personIdent = personident,
                            veilederIdent = VEILEDER_IDENT,
                            tekst = "En oppfolgingsoppgave",
                            oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
                        )
                        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                    }
                }

                it("Gets all oppfolgingsoppgaver for all persons") {
                    createOppfolgingsoppgaver()

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO =
                            objectMapper.readValue<OppfolgingsoppgaverResponseDTO>(response.content!!)

                        responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                        responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                        responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgave) ->
                            oppfolgingsoppgave.createdBy shouldBeEqualTo VEILEDER_IDENT
                            oppfolgingsoppgave.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }

                it("Gets only newest versjon of oppfolgingsoppgaver for all persons") {
                    val oppfolgingsoppgaver = createOppfolgingsoppgaver()
                    oppfolgingsoppgaveService.addVersion(
                        existingOppfolgingsoppgaveUuid = oppfolgingsoppgaver[0].uuid,
                        veilederIdent = VEILEDER_IDENT,
                        newTekst = "Ny tekst",
                        newFrist = LocalDate.now().plusDays(1),
                    )

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO =
                            objectMapper.readValue<OppfolgingsoppgaverResponseDTO>(response.content!!)

                        responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                        responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                        responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgave) ->
                            oppfolgingsoppgave.createdBy shouldBeEqualTo VEILEDER_IDENT
                            oppfolgingsoppgave.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                        val updatedOppgaver = responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgave) ->
                            oppfolgingsoppgave.tekst == "Ny tekst"
                        }.values
                        updatedOppgaver.size shouldBeEqualTo 1
                        updatedOppgaver.first().uuid shouldBeEqualTo oppfolgingsoppgaver[0].uuid.toString()

                        responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgave) ->
                            oppfolgingsoppgave.tekst == "En oppfolgingsoppgave"
                        }.size shouldBeEqualTo 2
                    }
                }

                it("Gets oppfolgingsoppgaver only for person with oppgaver, even when veileder has access to all persons") {
                    createOppfolgingsoppgaver(identer = listOf(ARBEIDSTAKER_PERSONIDENT))

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO =
                            objectMapper.readValue<OppfolgingsoppgaverResponseDTO>(response.content!!)

                        responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 1
                        responseDTO.oppfolgingsoppgaver.forEach { (personident, oppfolgingsoppgave) ->
                            personident shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT.value
                            oppfolgingsoppgave.createdBy shouldBeEqualTo VEILEDER_IDENT
                            oppfolgingsoppgave.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }

                it("Gets all oppfolgingsoppgaver only for persons where veileder has access") {
                    createOppfolgingsoppgaver()

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validTokenOtherVeileder))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO =
                            objectMapper.readValue<OppfolgingsoppgaverResponseDTO>(response.content!!)

                        responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 2
                        responseDTO.oppfolgingsoppgaver.keys shouldNotContain ARBEIDSTAKER_3_FNR
                        responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgave) ->
                            oppfolgingsoppgave.createdBy shouldBeEqualTo VEILEDER_IDENT
                            oppfolgingsoppgave.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }

                it("Gets all oppfolgingsoppgaver and send no duplicates when duplicates personident are sent in request") {
                    createOppfolgingsoppgaver(identer = listOf(*personidenter.toTypedArray(), ARBEIDSTAKER_PERSONIDENT))

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO =
                            objectMapper.readValue<OppfolgingsoppgaverResponseDTO>(response.content!!)

                        responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                        responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                        responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgave) ->
                            oppfolgingsoppgave.createdBy shouldBeEqualTo VEILEDER_IDENT
                            oppfolgingsoppgave.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }

                it("Gets no oppfolgingsoppgaver when veileder doesn't have access to any of the persons") {
                    val validTokenAccessToNoneVeileder = generateJWT(
                        audience = externalMockEnvironment.environment.azure.appClientId,
                        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                        navIdent = FAILS_VEILEDER_IDENT,
                    )

                    createOppfolgingsoppgaver()

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validTokenAccessToNoneVeileder))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("Gets no oppfolgingsoppgaver when none of the persons has oppfolgingsoppgaver") {
                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("Gets no oppfolgingsoppgaver when veileder has access but no active oppfolgingsoppgave") {
                    val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                        personIdent = ARBEIDSTAKER_PERSONIDENT,
                        veilederIdent = VEILEDER_IDENT,
                        tekst = "En oppfolgingsoppgave",
                        oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
                    )
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave.copy(isActive = false))

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                ARBEIDSTAKER_PERSONIDENT.value.drop(1)
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
