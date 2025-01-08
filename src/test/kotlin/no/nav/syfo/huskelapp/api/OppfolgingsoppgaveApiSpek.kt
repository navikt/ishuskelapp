package no.nav.syfo.huskelapp.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.syfo.api.endpoints.RequestParameters.IS_ACTIVE
import no.nav.syfo.api.endpoints.huskelappApiBasePath
import no.nav.syfo.api.model.*
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.PersonIdent
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
import no.nav.syfo.util.configure
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.util.*

class OppfolgingsoppgaveApiSpek : Spek({

    describe(OppfolgingsoppgaveApiSpek::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
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
                oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
            )
            val inactiveOppfolgingsoppgave = oppfolgingsoppgave.copy(
                uuid = UUID.randomUUID(),
                isActive = false
            )

            describe("Happy path") {
                val activeOppfolgingsoppgaveUrl = "$huskelappApiBasePath?$IS_ACTIVE=true"
                it("Returns OK if active oppfolgingsoppgave exists") {
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(activeOppfolgingsoppgaveUrl) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()
                        responseDTO.isActive shouldBe true
                        responseDTO.versjoner.size shouldBeEqualTo 1
                    }
                }
                it("Returns no content if no oppfolgingsoppgave exists") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(activeOppfolgingsoppgaveUrl) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
                it("Returns no content if no active oppfolgingsoppgave exists") {
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = inactiveOppfolgingsoppgave)

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(activeOppfolgingsoppgaveUrl) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.NoContent
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

            fun createOppfolgingsoppgave(tekst: String = "En oppfolgingsoppgave"): Oppfolgingsoppgave =
                Oppfolgingsoppgave.create(
                    personIdent = ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = VEILEDER_IDENT,
                    tekst = tekst,
                    oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
                )

            it("Oppretting av oppfølgingsoppgave skal ha 1 versjon") {
                oppfolgingsoppgaveRepository.create(createOppfolgingsoppgave())

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(huskelappApiBasePath) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK

                    val responseDTO = response.body<List<OppfolgingsoppgaveResponseDTO>>()

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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(huskelappApiBasePath) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK

                    val responseDTO = response.body<List<OppfolgingsoppgaveResponseDTO>>()

                    responseDTO.size shouldBeEqualTo 2

                    val sisteOppfolgingsoppgave = responseDTO[0]
                    val sisteVersjon = sisteOppfolgingsoppgave.versjoner.first()

                    sisteOppfolgingsoppgave.personIdent shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT
                    sisteOppfolgingsoppgave.isActive shouldBe true
                    sisteOppfolgingsoppgave.createdAt shouldBeEqualTo sisteOppfolgingsoppgave.updatedAt
                    sisteOppfolgingsoppgave.versjoner.size shouldBeEqualTo 1

                    sisteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                    sisteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                    sisteVersjon.tekst shouldBeEqualTo "Oppfølgingsoppgave nr. 2"

                    val forsteOppfolgingsoppgave = responseDTO[1]
                    val forsteVersjon = forsteOppfolgingsoppgave.versjoner.first()

                    forsteOppfolgingsoppgave.personIdent shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT
                    forsteOppfolgingsoppgave.isActive shouldBe false
                    forsteOppfolgingsoppgave.versjoner.size shouldBeEqualTo 1

                    forsteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                    forsteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                    forsteVersjon.tekst shouldBeEqualTo "Oppfølgingsoppgave nr. 1"
                }
            }

            it("En oppfølgingsoppgave med 2 versjoner") {
                val existingOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(createOppfolgingsoppgave())
                val oppdatertOppfolgingsoppgave = existingOppfolgingsoppgave.edit(
                    tekst = "En oppfolgingsoppgave oppdatert",
                    veilederIdent = VEILEDER_IDENT,
                    frist = null
                )
                val pExistingOppgave = oppfolgingsoppgaveRepository.getPOppfolgingsoppgave(existingOppfolgingsoppgave.uuid)
                oppfolgingsoppgaveRepository.updateOppfolgingsoppgaveMedVersjon(
                    pExistingOppgave!!.id,
                    oppdatertOppfolgingsoppgave
                )

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(huskelappApiBasePath) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK

                    val responseDTO = response.body<List<OppfolgingsoppgaveResponseDTO>>()

                    responseDTO.size shouldBeEqualTo 1

                    val oppfolgingsoppgave = responseDTO.first()
                    val forsteVersjon = oppfolgingsoppgave.versjoner[1]
                    val sisteVersjon = oppfolgingsoppgave.versjoner[0]

                    forsteVersjon.createdAt shouldBeEqualTo oppfolgingsoppgave.createdAt
                    forsteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                    forsteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                    forsteVersjon.tekst shouldBeEqualTo "En oppfolgingsoppgave"
                    forsteVersjon.frist.shouldBeNull()

                    sisteVersjon.createdAt shouldBeEqualTo oppfolgingsoppgave.updatedAt
                    sisteVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                    sisteVersjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                    sisteVersjon.tekst shouldBeEqualTo "En oppfolgingsoppgave oppdatert"
                    sisteVersjon.frist.shouldBeNull()

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
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(huskelappApiBasePath) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            contentType(ContentType.Application.Json)
                            setBody(requestDTOWithTekst)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                        val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                        oppfolgingsoppgaveVersjon.tekst shouldBeEqualTo requestDTOWithTekst.tekst
                        oppfolgingsoppgaveVersjon.oppfolgingsgrunn shouldBeEqualTo requestDTOWithTekst.oppfolgingsgrunn
                        oppfolgingsoppgaveVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                    }

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                        responseDTO.versjoner.size shouldBeEqualTo 1

                        val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                        oppfolgingsoppgaveVersjon.tekst shouldBeEqualTo requestDTOWithTekst.tekst
                        oppfolgingsoppgaveVersjon.oppfolgingsgrunn shouldBeEqualTo requestDTOWithTekst.oppfolgingsgrunn
                        oppfolgingsoppgaveVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                    }
                }
                it("OK with oppfolgingsgrunn") {
                    val requestDTOWithOppfolgingsgrunn = OppfolgingsoppgaveRequestDTO(
                        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_ANNEN_YTELSE,
                        tekst = null,
                        frist = null,
                    )
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(huskelappApiBasePath) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            contentType(ContentType.Application.Json)
                            setBody(requestDTOWithOppfolgingsgrunn)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Created
                    }

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                        val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                        oppfolgingsoppgaveVersjon.tekst shouldBeEqualTo requestDTOWithOppfolgingsgrunn.tekst
                        oppfolgingsoppgaveVersjon.oppfolgingsgrunn shouldBeEqualTo requestDTOWithOppfolgingsgrunn.oppfolgingsgrunn
                        oppfolgingsoppgaveVersjon.frist shouldBeEqualTo requestDTOWithOppfolgingsgrunn.frist
                        oppfolgingsoppgaveVersjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                    }
                }
                it("Does not store unchanged") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.post(huskelappApiBasePath) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            contentType(ContentType.Application.Json)
                            setBody(requestDTO)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.Created
                        }
                        client.post(huskelappApiBasePath) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            contentType(ContentType.Application.Json)
                            setBody(requestDTO)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.Created
                        }

                        val oppfolgingsoppgave =
                            oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(ARBEIDSTAKER_PERSONIDENT)
                                .first()
                        oppfolgingsoppgaveRepository.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgave.id).size shouldBeEqualTo 1
                    }
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
                        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
                        frist = LocalDate.now().minusDays(1)
                    )
                    val existingOppfolgingsoppgave =
                        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                    val requestDTO = EditedOppfolgingsoppgaveDTO(
                        tekst = existingOppfolgingsoppgave.sisteVersjon().tekst,
                        frist = LocalDate.now().plusDays(1),
                    )

                    testApplication {
                        val client = setupApiAndClient()
                        client.post("$huskelappApiBasePath/${existingOppfolgingsoppgave.uuid}") {
                            bearerAuth(validTokenOtherVeileder)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            contentType(ContentType.Application.Json)
                            setBody(requestDTO)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.Created
                        }

                        val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                        val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                        oppfolgingsoppgaveVersjon.tekst shouldBeEqualTo requestDTO.tekst
                        oppfolgingsoppgaveVersjon.frist shouldBeEqualTo requestDTO.frist
                        oppfolgingsoppgaveVersjon.createdBy shouldBeEqualTo OTHER_VEILEDER_IDENT
                    }
                }
                it("OK with new tekst and same veileder") {
                    val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                        personIdent = ARBEIDSTAKER_PERSONIDENT,
                        veilederIdent = VEILEDER_IDENT,
                        tekst = "En oppfolgingsoppgave",
                        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
                        frist = LocalDate.now().minusDays(1)
                    )
                    val existingOppfolgingsoppgave =
                        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                    val requestDTO = EditedOppfolgingsoppgaveDTO(
                        tekst = "Ny tekst",
                        frist = existingOppfolgingsoppgave.sisteVersjon().frist,
                    )

                    testApplication {
                        val client = setupApiAndClient()
                        client.post("$huskelappApiBasePath/${existingOppfolgingsoppgave.uuid}") {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            contentType(ContentType.Application.Json)
                            setBody(requestDTO)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.Created
                        }

                        val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()
                        val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                        oppfolgingsoppgaveVersjon.tekst shouldBeEqualTo requestDTO.tekst
                        oppfolgingsoppgaveVersjon.frist shouldBeEqualTo requestDTO.frist
                        oppfolgingsoppgaveVersjon.createdBy shouldBeEqualTo oppfolgingsoppgave.sisteVersjon().createdBy
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post("$huskelappApiBasePath/${UUID.randomUUID()}") {
                            bearerAuth(validTokenOtherVeileder)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                            contentType(ContentType.Application.Json)
                            setBody(requestDTO)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.NotFound
                    }
                }
            }
        }
        describe("Delete oppfolgingsoppgave") {
            val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                personIdent = ARBEIDSTAKER_PERSONIDENT,
                veilederIdent = VEILEDER_IDENT,
                tekst = "En oppfolgingsoppgave",
                oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
            )
            val inactiveOppfolgingsoppgave = oppfolgingsoppgave.copy(
                uuid = UUID.randomUUID(),
                isActive = false
            )

            val deleteHuskelappUrl = "$huskelappApiBasePath/${oppfolgingsoppgave.uuid}"

            describe("Happy path") {
                it("returns NoContent and sets oppfolgingsoppgave inactive") {
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.delete(deleteHuskelappUrl) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.NoContent
                    }

                    val oppfolgingsoppgaver =
                        oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(ARBEIDSTAKER_PERSONIDENT)
                    oppfolgingsoppgaver.size shouldBeEqualTo 1
                    oppfolgingsoppgaver.first().isActive.shouldBeFalse()
                }
            }
            describe("Unhappy path") {
                it("returns status NotFound if no oppfolgingsoppgave exists for given uuid") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.delete("$huskelappApiBasePath/${UUID.randomUUID()}") {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.NotFound
                    }
                }
                it("returns status NotFound if no active oppfolgingsoppgave exists for given uuid") {
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = inactiveOppfolgingsoppgave)

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.delete("$huskelappApiBasePath/${inactiveOppfolgingsoppgave.uuid}") {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.NotFound
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
            val personidenter =
                listOf(ARBEIDSTAKER_PERSONIDENT, ARBEIDSTAKER_2_PERSONIDENT, ARBEIDSTAKER_3_PERSONIDENT)
            val requestDTO = OppfolgingsoppgaverRequestDTO(personidenter.map { it.value })
            val url = "$huskelappApiBasePath/get-oppfolgingsoppgaver"

            fun createOppfolgingsoppgaver(identer: List<PersonIdent> = personidenter): List<Oppfolgingsoppgave> {
                return identer.map { personident ->
                    val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                        personIdent = personident,
                        veilederIdent = VEILEDER_IDENT,
                        tekst = "En oppfolgingsoppgave",
                        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                    )
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                }
            }

            it("Gets all oppfolgingsoppgaver for all persons") {
                createOppfolgingsoppgaver()

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                    responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }
            }

            it("Gets only newest versjon of oppfolgingsoppgaver for all persons") {
                val oppfolgingsoppgaver = createOppfolgingsoppgaver()
                oppfolgingsoppgaveService.editOppfolgingsoppgave(
                    existingOppfolgingsoppgaveUuid = oppfolgingsoppgaver[0].uuid,
                    veilederIdent = VEILEDER_IDENT,
                    newTekst = "Ny tekst",
                    newFrist = LocalDate.now().plusDays(1),
                )

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                    responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                    val updatedOppgaver =
                        responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgaveResponseDTO) ->
                            oppfolgingsoppgaveResponseDTO.versjoner.first().tekst == "Ny tekst"
                        }.values
                    updatedOppgaver.size shouldBeEqualTo 1
                    updatedOppgaver.first().uuid shouldBeEqualTo oppfolgingsoppgaver[0].uuid.toString()

                    responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.first().tekst == "En oppfolgingsoppgave"
                    }.size shouldBeEqualTo 2
                }
            }

            it("Gets oppfolgingsoppgaver only for person with oppgaver, even when veileder has access to all persons") {
                createOppfolgingsoppgaver(identer = listOf(ARBEIDSTAKER_PERSONIDENT))

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }
                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 1
                    responseDTO.oppfolgingsoppgaver.forEach { (personident, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            personident shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT.value
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }
            }

            it("Gets all oppfolgingsoppgaver only for persons where veileder has access") {
                createOppfolgingsoppgaver()

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validTokenOtherVeileder)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }
                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 2
                    responseDTO.oppfolgingsoppgaver.keys shouldNotContain ARBEIDSTAKER_3_FNR
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }
            }

            it("Gets all oppfolgingsoppgaver and send no duplicates when duplicates personident are sent in request") {
                createOppfolgingsoppgaver(identer = listOf(*personidenter.toTypedArray(), ARBEIDSTAKER_PERSONIDENT))

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                    responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validTokenAccessToNoneVeileder)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("Gets no oppfolgingsoppgaver when none of the persons has oppfolgingsoppgaver") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("Gets no oppfolgingsoppgaver when veileder has access but no active oppfolgingsoppgave") {
                val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                    personIdent = ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = VEILEDER_IDENT,
                    tekst = "En oppfolgingsoppgave",
                    oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                )
                oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave.copy(isActive = false))

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }
        }

        describe("POST: Get oppfolgingsoppgaverNew for several persons") {
            val personidenter =
                listOf(ARBEIDSTAKER_PERSONIDENT, ARBEIDSTAKER_2_PERSONIDENT, ARBEIDSTAKER_3_PERSONIDENT)
            val requestDTO = OppfolgingsoppgaverRequestDTO(personidenter.map { it.value })
            val url = "$huskelappApiBasePath/get-oppfolgingsoppgaver-new"

            fun createOppfolgingsoppgaver(identer: List<PersonIdent> = personidenter): List<Oppfolgingsoppgave> {
                return identer.map { personident ->
                    val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                        personIdent = personident,
                        veilederIdent = VEILEDER_IDENT,
                        tekst = "En oppfolgingsoppgave",
                        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                    )
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                }
            }

            it("Gets all oppfolgingsoppgaver for all persons") {
                createOppfolgingsoppgaver()

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                    responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }
            }

            it("Gets only newest versjon of oppfolgingsoppgaver for all persons") {
                val oppfolgingsoppgaver = createOppfolgingsoppgaver()
                oppfolgingsoppgaveService.editOppfolgingsoppgave(
                    existingOppfolgingsoppgaveUuid = oppfolgingsoppgaver[0].uuid,
                    veilederIdent = VEILEDER_IDENT,
                    newTekst = "Ny tekst",
                    newFrist = LocalDate.now().plusDays(1),
                )

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                    responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                    val updatedOppgaver =
                        responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgaveResponseDTO) ->
                            oppfolgingsoppgaveResponseDTO.versjoner.first().tekst == "Ny tekst"
                        }.values
                    updatedOppgaver.size shouldBeEqualTo 1
                    updatedOppgaver.first().uuid shouldBeEqualTo oppfolgingsoppgaver[0].uuid.toString()

                    responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.first().tekst == "En oppfolgingsoppgave"
                    }.size shouldBeEqualTo 2
                }
            }

            it("Gets oppfolgingsoppgaver only for person with oppgaver, even when veileder has access to all persons") {
                createOppfolgingsoppgaver(identer = listOf(ARBEIDSTAKER_PERSONIDENT))

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 1
                    responseDTO.oppfolgingsoppgaver.forEach { (personident, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            personident shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT.value
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }
            }

            it("Gets all oppfolgingsoppgaver only for persons where veileder has access") {
                createOppfolgingsoppgaver()

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validTokenOtherVeileder)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 2
                    responseDTO.oppfolgingsoppgaver.keys shouldNotContain ARBEIDSTAKER_3_FNR
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
                    }
                }
            }

            it("Gets all oppfolgingsoppgaver and send no duplicates when duplicates personident are sent in request") {
                createOppfolgingsoppgaver(identer = listOf(*personidenter.toTypedArray(), ARBEIDSTAKER_PERSONIDENT))

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                    responseDTO.oppfolgingsoppgaver.size shouldBeEqualTo 3
                    responseDTO.oppfolgingsoppgaver.keys shouldContainAll personidenter.map { it.value }
                    responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                            versjon.createdBy shouldBeEqualTo VEILEDER_IDENT
                            versjon.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                        }
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

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validTokenAccessToNoneVeileder)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("Gets no oppfolgingsoppgaver when none of the persons has oppfolgingsoppgaver") {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("Gets no oppfolgingsoppgaver when veileder has access but no active oppfolgingsoppgave") {
                val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                    personIdent = ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = VEILEDER_IDENT,
                    tekst = "En oppfolgingsoppgave",
                    oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                )
                oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave.copy(isActive = false))

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }

                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }
        }
    }
})

fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
    application {
        testApiModule(
            externalMockEnvironment = ExternalMockEnvironment.instance,
        )
    }
    val client = createClient {
        install(ContentNegotiation) {
            jackson { configure() }
        }
    }
    return client
}

private fun testMissingToken(url: String, httpMethod: HttpMethod) {
    testApplication {
        val client = setupApiAndClient()
        val response = when (httpMethod) {
            HttpMethod.Post -> client.post(url) {}
            HttpMethod.Delete -> client.delete(url) {}
            else -> client.get(url) {}
        }
        response.status shouldBeEqualTo HttpStatusCode.Unauthorized
    }
}

private fun testMissingPersonIdent(
    url: String,
    validToken: String,
    httpMethod: HttpMethod,
) {
    testApplication {
        val client = setupApiAndClient()
        val response = when (httpMethod) {
            HttpMethod.Post -> client.post(url) { bearerAuth(validToken) }
            HttpMethod.Delete -> client.delete(url) { bearerAuth(validToken) }
            else -> client.get(url) { bearerAuth(validToken) }
        }
        response.status shouldBeEqualTo HttpStatusCode.BadRequest
    }
}

private fun testInvalidPersonIdent(
    url: String,
    validToken: String,
    httpMethod: HttpMethod,
) {
    testApplication {
        val client = setupApiAndClient()
        val response = when (httpMethod) {
            HttpMethod.Post -> client.post(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value.drop(1))
            }
            HttpMethod.Delete -> client.delete(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value.drop(1))
            }
            else -> client.get(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value.drop(1))
            }
        }
        response.status shouldBeEqualTo HttpStatusCode.BadRequest
    }
}

private fun testDeniedPersonAccess(
    url: String,
    validToken: String,
    httpMethod: HttpMethod,
) {
    testApplication {
        val client = setupApiAndClient()
        val response = when (httpMethod) {
            HttpMethod.Post -> client.post(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS.value)
            }
            HttpMethod.Delete -> client.delete(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS.value)
            }
            else -> client.get(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS.value)
            }
        }
        response.status shouldBeEqualTo HttpStatusCode.Forbidden
    }
}
