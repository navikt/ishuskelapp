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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertNull

class OppfolgingsoppgaveApiTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(
        database = database,
    )
    private val oppfolgingsoppgaveService = OppfolgingsoppgaveService(
        oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
    )

    @AfterEach
    fun tearDown() {
        database.dropData()
    }

    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        navIdent = VEILEDER_IDENT,
    )
    private val validTokenOtherVeileder = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        navIdent = OTHER_VEILEDER_IDENT,
    )

    @Nested
    @DisplayName("Get oppfolgingsoppgave for person")
    inner class GetOppfolgingsoppgaveForPerson {
        private val oppfolgingsoppgave = Oppfolgingsoppgave.create(
            personIdent = ARBEIDSTAKER_PERSONIDENT,
            veilederIdent = VEILEDER_IDENT,
            tekst = "En oppfolgingsoppgave",
            oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
        )
        private val inactiveOppfolgingsoppgave = oppfolgingsoppgave.copy(
            uuid = UUID.randomUUID(),
            isActive = false
        )

        @Nested
        @DisplayName("Happy path")
        inner class HappyPath {
            private val activeOppfolgingsoppgaveUrl = "$huskelappApiBasePath?$IS_ACTIVE=true"

            @Test
            fun `Returns OK if active oppfolgingsoppgave exists`() {
                oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(activeOppfolgingsoppgaveUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()
                    assertTrue(responseDTO.isActive)
                    assertEquals(1, responseDTO.versjoner.size)
                }
            }

            @Test
            fun `Returns no content if no oppfolgingsoppgave exists`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(activeOppfolgingsoppgaveUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.NoContent, response.status)
                }
            }

            @Test
            fun `Returns no content if no active oppfolgingsoppgave exists`() {
                oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = inactiveOppfolgingsoppgave)

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(activeOppfolgingsoppgaveUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.NoContent, response.status)
                }
            }
        }

        @Nested
        @DisplayName("Unhappy path")
        inner class UnhappyPath {
            @Test
            fun `Returns status Unauthorized if no token is supplied`() {
                testMissingToken(huskelappApiBasePath, HttpMethod.Get)
            }

            @Test
            fun `returns status Forbidden if denied access to person`() {
                testDeniedPersonAccess(huskelappApiBasePath, validToken, HttpMethod.Get)
            }

            @Test
            fun `returns status BadRequest if no NAV_PERSONIDENT_HEADER is supplied`() {
                testMissingPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Get)
            }

            @Test
            fun `returns status BadRequest if NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() {
                testInvalidPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Get)
            }
        }
    }

    @Nested
    @DisplayName("Get oppfølgingsoppgaver med versjoner")
    inner class GetOppfolgingsoppgaverMedVersjoner {

        fun createOppfolgingsoppgave(tekst: String = "En oppfolgingsoppgave"): Oppfolgingsoppgave =
            Oppfolgingsoppgave.create(
                personIdent = ARBEIDSTAKER_PERSONIDENT,
                veilederIdent = VEILEDER_IDENT,
                tekst = tekst,
                oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
            )

        @Test
        fun `Oppretting av oppfølgingsoppgave skal ha 1 versjon`() {
            oppfolgingsoppgaveRepository.create(createOppfolgingsoppgave())

            testApplication {
                val client = setupApiAndClient()
                val response = client.get(huskelappApiBasePath) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                }

                assertEquals(HttpStatusCode.OK, response.status)

                val responseDTO = response.body<List<OppfolgingsoppgaveResponseDTO>>()

                assertEquals(1, responseDTO.size)

                val oppfolgingsoppgave = responseDTO.first()
                val sisteVersjon = oppfolgingsoppgave.versjoner.first()

                assertEquals(ARBEIDSTAKER_PERSONIDENT, oppfolgingsoppgave.personIdent)
                assertTrue(oppfolgingsoppgave.isActive)
                assertEquals(oppfolgingsoppgave.updatedAt, oppfolgingsoppgave.createdAt)
                assertEquals(1, oppfolgingsoppgave.versjoner.size)

                assertEquals(oppfolgingsoppgave.updatedAt, sisteVersjon.createdAt)
                assertEquals(VEILEDER_IDENT, sisteVersjon.createdBy)
                assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, sisteVersjon.oppfolgingsgrunn)
                assertEquals("En oppfolgingsoppgave", sisteVersjon.tekst)
            }
        }

        @Test
        fun `Oppretting av 2 oppfølgingsoppgaver skal ha 1 versjon hver`() {
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

                assertEquals(HttpStatusCode.OK, response.status)

                val responseDTO = response.body<List<OppfolgingsoppgaveResponseDTO>>()

                assertEquals(2, responseDTO.size)

                val sisteOppfolgingsoppgave = responseDTO[0]
                val sisteVersjon = sisteOppfolgingsoppgave.versjoner.first()

                assertEquals(ARBEIDSTAKER_PERSONIDENT, sisteOppfolgingsoppgave.personIdent)
                assertTrue(sisteOppfolgingsoppgave.isActive)
                assertEquals(sisteOppfolgingsoppgave.updatedAt, sisteOppfolgingsoppgave.createdAt)
                assertEquals(1, sisteOppfolgingsoppgave.versjoner.size)

                assertEquals(VEILEDER_IDENT, sisteVersjon.createdBy)
                assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, sisteVersjon.oppfolgingsgrunn)
                assertEquals("Oppfølgingsoppgave nr. 2", sisteVersjon.tekst)

                val forsteOppfolgingsoppgave = responseDTO[1]
                val forsteVersjon = forsteOppfolgingsoppgave.versjoner.first()

                assertEquals(ARBEIDSTAKER_PERSONIDENT, forsteOppfolgingsoppgave.personIdent)
                assertFalse(forsteOppfolgingsoppgave.isActive)
                assertEquals(1, forsteOppfolgingsoppgave.versjoner.size)

                assertEquals(VEILEDER_IDENT, forsteVersjon.createdBy)
                assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, forsteVersjon.oppfolgingsgrunn)
                assertEquals("Oppfølgingsoppgave nr. 1", forsteVersjon.tekst)
            }
        }

        @Test
        fun `En oppfølgingsoppgave med 2 versjoner`() {
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

                assertEquals(HttpStatusCode.OK, response.status)

                val responseDTO = response.body<List<OppfolgingsoppgaveResponseDTO>>()

                assertEquals(1, responseDTO.size)

                val oppfolgingsoppgave = responseDTO.first()
                val forsteVersjon = oppfolgingsoppgave.versjoner[1]
                val sisteVersjon = oppfolgingsoppgave.versjoner[0]

                assertEquals(oppfolgingsoppgave.createdAt, forsteVersjon.createdAt)
                assertEquals(VEILEDER_IDENT, forsteVersjon.createdBy)
                assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, forsteVersjon.oppfolgingsgrunn)
                assertEquals("En oppfolgingsoppgave", forsteVersjon.tekst)
                assertNull(forsteVersjon.frist)

                assertEquals(oppfolgingsoppgave.updatedAt, sisteVersjon.createdAt)
                assertEquals(VEILEDER_IDENT, sisteVersjon.createdBy)
                assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, sisteVersjon.oppfolgingsgrunn)
                assertEquals("En oppfolgingsoppgave oppdatert", sisteVersjon.tekst)
                assertNull(sisteVersjon.frist)

                assertEquals(ARBEIDSTAKER_PERSONIDENT, oppfolgingsoppgave.personIdent)
                assertTrue(oppfolgingsoppgave.isActive)
                assertEquals(2, oppfolgingsoppgave.versjoner.size)
                assertEquals(forsteVersjon.createdAt, oppfolgingsoppgave.createdAt)
                assertEquals(sisteVersjon.createdAt, oppfolgingsoppgave.updatedAt)
            }
        }
    }

    @Nested
    @DisplayName("Post oppfolgingsoppgave")
    inner class PostOppfolgingsoppgave {
        @Nested
        @DisplayName("Happy path")
        inner class HappyPath {
            private val requestDTO = OppfolgingsoppgaveRequestDTO(
                tekst = "En tekst",
                oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
                frist = LocalDate.now().plusDays(1),
            )

            @Test
            fun `OK with tekst`() {
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

                    assertEquals(HttpStatusCode.Created, response.status)
                    val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                    val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                    assertEquals(requestDTOWithTekst.tekst, oppfolgingsoppgaveVersjon.tekst)
                    assertEquals(requestDTOWithTekst.oppfolgingsgrunn, oppfolgingsoppgaveVersjon.oppfolgingsgrunn)
                    assertEquals(VEILEDER_IDENT, oppfolgingsoppgaveVersjon.createdBy)
                }

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                    assertEquals(1, responseDTO.versjoner.size)

                    val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                    assertEquals(requestDTOWithTekst.tekst, oppfolgingsoppgaveVersjon.tekst)
                    assertEquals(requestDTOWithTekst.oppfolgingsgrunn, oppfolgingsoppgaveVersjon.oppfolgingsgrunn)
                    assertEquals(VEILEDER_IDENT, oppfolgingsoppgaveVersjon.createdBy)
                }
            }

            @Test
            fun `OK with oppfolgingsgrunn`() {
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

                    assertEquals(HttpStatusCode.Created, response.status)
                }

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                    val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                    assertEquals(requestDTOWithOppfolgingsgrunn.tekst, oppfolgingsoppgaveVersjon.tekst)
                    assertEquals(requestDTOWithOppfolgingsgrunn.oppfolgingsgrunn, oppfolgingsoppgaveVersjon.oppfolgingsgrunn)
                    assertEquals(requestDTOWithOppfolgingsgrunn.frist, oppfolgingsoppgaveVersjon.frist)
                    assertEquals(VEILEDER_IDENT, oppfolgingsoppgaveVersjon.createdBy)
                }
            }

            @Test
            fun `Does not store unchanged`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.post(huskelappApiBasePath) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }.apply {
                        assertEquals(HttpStatusCode.Created, status)
                    }
                    client.post(huskelappApiBasePath) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                        contentType(ContentType.Application.Json)
                        setBody(requestDTO)
                    }.apply {
                        assertEquals(HttpStatusCode.Created, status)
                    }

                    val oppfolgingsoppgave =
                        oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(ARBEIDSTAKER_PERSONIDENT)
                            .first()
                    assertEquals(1, oppfolgingsoppgaveRepository.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgave.id).size)
                }
            }
        }

        @Nested
        @DisplayName("Unhappy path")
        inner class UnhappyPath {
            @Test
            fun `Returns status Unauthorized if no token is supplied`() {
                testMissingToken(huskelappApiBasePath, HttpMethod.Post)
            }

            @Test
            fun `returns status Forbidden if denied access to person`() {
                testDeniedPersonAccess(huskelappApiBasePath, validToken, HttpMethod.Post)
            }

            @Test
            fun `returns status BadRequest if no NAV_PERSONIDENT_HEADER is supplied`() {
                testMissingPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Post)
            }

            @Test
            fun `returns status BadRequest if NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() {
                testInvalidPersonIdent(huskelappApiBasePath, validToken, HttpMethod.Post)
            }
        }
    }

    @Nested
    @DisplayName("Post new version of oppfolgingsoppgave")
    inner class PostNewVersionOfOppfolgingsoppgave {
        @Nested
        @DisplayName("Happy path")
        inner class HappyPath {
            @Test
            fun `OK with new date and other veileder`() {
                val oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                    personIdent = ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = VEILEDER_IDENT,
                    tekst = "En oppfolgingsoppgave",
                    oppfolgingsgrunn = oppfolgingsgrunn,
                    frist = LocalDate.now().minusDays(1)
                )
                val existingOppfolgingsoppgave =
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                val requestDTO = EditedOppfolgingsoppgaveDTO(
                    oppfolgingsgrunn = oppfolgingsgrunn,
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
                        assertEquals(HttpStatusCode.Created, status)
                    }

                    val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()

                    val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                    assertEquals(requestDTO.tekst, oppfolgingsoppgaveVersjon.tekst)
                    assertEquals(requestDTO.frist, oppfolgingsoppgaveVersjon.frist)
                    assertEquals(OTHER_VEILEDER_IDENT, oppfolgingsoppgaveVersjon.createdBy)
                }
            }

            @Test
            fun `OK with new tekst and same veileder`() {
                val oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                val oppfolgingsoppgave = Oppfolgingsoppgave.create(
                    personIdent = ARBEIDSTAKER_PERSONIDENT,
                    veilederIdent = VEILEDER_IDENT,
                    tekst = "En oppfolgingsoppgave",
                    oppfolgingsgrunn = oppfolgingsgrunn,
                    frist = LocalDate.now().minusDays(1)
                )
                val existingOppfolgingsoppgave =
                    oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)
                val requestDTO = EditedOppfolgingsoppgaveDTO(
                    oppfolgingsgrunn = oppfolgingsgrunn,
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
                        assertEquals(HttpStatusCode.Created, status)
                    }

                    val response = client.get("$huskelappApiBasePath?$IS_ACTIVE=true") {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val responseDTO = response.body<OppfolgingsoppgaveResponseDTO>()
                    val oppfolgingsoppgaveVersjon = responseDTO.versjoner.first()

                    assertEquals(requestDTO.tekst, oppfolgingsoppgaveVersjon.tekst)
                    assertEquals(requestDTO.frist, oppfolgingsoppgaveVersjon.frist)
                    assertEquals(oppfolgingsoppgave.sisteVersjon().createdBy, oppfolgingsoppgaveVersjon.createdBy)
                }
            }

            @Test
            fun `OK with new oppfolgingsgrunn`() {
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
                    oppfolgingsgrunn = Oppfolgingsgrunn.SAMTALE_MED_BRUKER,
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
                        assertEquals(HttpStatusCode.Created, status)
                    }

                    val response = client.get(huskelappApiBasePath) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val responseDTOs = response.body<List<OppfolgingsoppgaveResponseDTO>>()
                    assertEquals(2, responseDTOs.size)

                    val aktivOppfolgingsoppgave = responseDTOs.first()
                    assertTrue(aktivOppfolgingsoppgave.isActive)
                    assertEquals(1, aktivOppfolgingsoppgave.versjoner.size)
                    assertEquals(Oppfolgingsgrunn.SAMTALE_MED_BRUKER, aktivOppfolgingsoppgave.versjoner.first().oppfolgingsgrunn)

                    val inaktivOppfolgingsoppgave = responseDTOs.last()
                    assertFalse(inaktivOppfolgingsoppgave.isActive)
                    assertEquals(1, inaktivOppfolgingsoppgave.versjoner.size)
                    assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, inaktivOppfolgingsoppgave.versjoner.first().oppfolgingsgrunn)
                    assertEquals(existingOppfolgingsoppgave.uuid.toString(), inaktivOppfolgingsoppgave.uuid)
                }
            }
        }

        @Nested
        @DisplayName("Unhappy path")
        inner class UnhappyPath {
            @Test
            fun `Returns status NotFound if oppfolgingsoppgave does not exist`() {
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

                    assertEquals(HttpStatusCode.NotFound, response.status)
                }
            }
        }
    }

    @Nested
    @DisplayName("Delete oppfolgingsoppgave")
    inner class DeleteOppfolgingsoppgave {
        private val oppfolgingsoppgave = Oppfolgingsoppgave.create(
            personIdent = ARBEIDSTAKER_PERSONIDENT,
            veilederIdent = VEILEDER_IDENT,
            tekst = "En oppfolgingsoppgave",
            oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
        )
        private val inactiveOppfolgingsoppgave = oppfolgingsoppgave.copy(
            uuid = UUID.randomUUID(),
            isActive = false
        )

        private val deleteHuskelappUrl = "$huskelappApiBasePath/${oppfolgingsoppgave.uuid}"

        @Nested
        @DisplayName("Happy path")
        inner class HappyPath {
            @Test
            fun `returns NoContent and sets oppfolgingsoppgave inactive`() {
                oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgave)

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.delete(deleteHuskelappUrl) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }
                    assertEquals(HttpStatusCode.NoContent, response.status)
                }

                val oppfolgingsoppgaver =
                    oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(ARBEIDSTAKER_PERSONIDENT)
                assertEquals(1, oppfolgingsoppgaver.size)
                assertFalse(oppfolgingsoppgaver.first().isActive)
            }
        }

        @Nested
        @DisplayName("Unhappy path")
        inner class UnhappyPath {
            @Test
            fun `returns status NotFound if no oppfolgingsoppgave exists for given uuid`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.delete("$huskelappApiBasePath/${UUID.randomUUID()}") {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }
                    assertEquals(HttpStatusCode.NotFound, response.status)
                }
            }

            @Test
            fun `returns status NotFound if no active oppfolgingsoppgave exists for given uuid`() {
                oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = inactiveOppfolgingsoppgave)

                testApplication {
                    val client = setupApiAndClient()
                    val response = client.delete("$huskelappApiBasePath/${inactiveOppfolgingsoppgave.uuid}") {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                    }
                    assertEquals(HttpStatusCode.NotFound, response.status)
                }
            }

            @Test
            fun `returns status Unauthorized if no token is supplied`() {
                testMissingToken(deleteHuskelappUrl, HttpMethod.Delete)
            }

            @Test
            fun `returns status Forbidden if denied access to person`() {
                testDeniedPersonAccess(deleteHuskelappUrl, validToken, HttpMethod.Delete)
            }

            @Test
            fun `returns status BadRequest if no NAV_PERSONIDENT_HEADER is supplied`() {
                testMissingPersonIdent(deleteHuskelappUrl, validToken, HttpMethod.Delete)
            }

            @Test
            fun `returns status BadRequest if NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied`() {
                testInvalidPersonIdent(deleteHuskelappUrl, validToken, HttpMethod.Delete)
            }
        }
    }

    @Nested
    @DisplayName("POST: Get oppfolgingsoppgaver for several persons")
    inner class PostGetOppfolgingsoppgaverForSeveralPersons {
        private val personidenter =
            listOf(ARBEIDSTAKER_PERSONIDENT, ARBEIDSTAKER_2_PERSONIDENT, ARBEIDSTAKER_3_PERSONIDENT)
        private val requestDTO = OppfolgingsoppgaverRequestDTO(personidenter.map { it.value })
        private val url = "$huskelappApiBasePath/get-oppfolgingsoppgaver"

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

        @Test
        fun `Gets all oppfolgingsoppgaver for all persons`() {
            createOppfolgingsoppgaver()

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                assertEquals(3, responseDTO.oppfolgingsoppgaver.size)
                assertTrue(responseDTO.oppfolgingsoppgaver.keys.containsAll(personidenter.map { it.value }))
                responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                    oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                        assertEquals(VEILEDER_IDENT, versjon.createdBy)
                        assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, versjon.oppfolgingsgrunn)
                    }
                }
            }
        }

        @Test
        fun `Gets only newest versjon of oppfolgingsoppgaver for all persons`() {
            val oppfolgingsoppgaver = createOppfolgingsoppgaver()
            oppfolgingsoppgaveService.editOppfolgingsoppgave(
                existingOppfolgingsoppgaveUuid = oppfolgingsoppgaver[0].uuid,
                veilederIdent = VEILEDER_IDENT,
                newOppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE,
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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                assertEquals(3, responseDTO.oppfolgingsoppgaver.size)
                assertTrue(responseDTO.oppfolgingsoppgaver.keys.containsAll(personidenter.map { it.value }))
                responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                    oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                        assertEquals(VEILEDER_IDENT, versjon.createdBy)
                        assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, versjon.oppfolgingsgrunn)
                    }
                }
                val updatedOppgaver =
                    responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.first().tekst == "Ny tekst"
                    }.values
                assertEquals(1, updatedOppgaver.size)
                assertEquals(oppfolgingsoppgaver[0].uuid.toString(), updatedOppgaver.first().uuid)

                assertEquals(
                    2,
                    responseDTO.oppfolgingsoppgaver.filter { (_, oppfolgingsoppgaveResponseDTO) ->
                        oppfolgingsoppgaveResponseDTO.versjoner.first().tekst == "En oppfolgingsoppgave"
                    }.size
                )
            }
        }

        @Test
        fun `Gets oppfolgingsoppgaver only for person with oppgaver, even when veileder has access to all persons`() {
            createOppfolgingsoppgaver(identer = listOf(ARBEIDSTAKER_PERSONIDENT))

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }
                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                assertEquals(1, responseDTO.oppfolgingsoppgaver.size)
                responseDTO.oppfolgingsoppgaver.forEach { (personident, oppfolgingsoppgaveResponseDTO) ->
                    oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                        assertEquals(ARBEIDSTAKER_PERSONIDENT.value, personident)
                        assertEquals(VEILEDER_IDENT, versjon.createdBy)
                        assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, versjon.oppfolgingsgrunn)
                    }
                }
            }
        }

        @Test
        fun `Gets all oppfolgingsoppgaver only for persons where veileder has access`() {
            createOppfolgingsoppgaver()

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validTokenOtherVeileder)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }
                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                assertEquals(2, responseDTO.oppfolgingsoppgaver.size)
                assertFalse(responseDTO.oppfolgingsoppgaver.keys.contains(ARBEIDSTAKER_3_FNR))
                responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                    oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                        assertEquals(VEILEDER_IDENT, versjon.createdBy)
                        assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, versjon.oppfolgingsgrunn)
                    }
                }
            }
        }

        @Test
        fun `Gets all oppfolgingsoppgaver and send no duplicates when duplicates personident are sent in request`() {
            createOppfolgingsoppgaver(identer = listOf(*personidenter.toTypedArray(), ARBEIDSTAKER_PERSONIDENT))

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()

                assertEquals(3, responseDTO.oppfolgingsoppgaver.size)
                assertTrue(responseDTO.oppfolgingsoppgaver.keys.containsAll(personidenter.map { it.value }))
                responseDTO.oppfolgingsoppgaver.forEach { (_, oppfolgingsoppgaveResponseDTO) ->
                    oppfolgingsoppgaveResponseDTO.versjoner.forEach { versjon ->
                        assertEquals(VEILEDER_IDENT, versjon.createdBy)
                        assertEquals(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE, versjon.oppfolgingsgrunn)
                    }
                }
            }
        }

        @Test
        fun `Gets no oppfolgingsoppgaver when veileder doesn't have access to any of the persons`() {
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

                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }

        @Test
        fun `Gets no oppfolgingsoppgaver when none of the persons has oppfolgingsoppgaver`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }

        @Test
        fun `Gets no oppfolgingsoppgaver when veileder has access but no active oppfolgingsoppgave`() {
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

                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }
    }
}

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
        assertEquals(HttpStatusCode.Unauthorized, response.status)
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
        assertEquals(HttpStatusCode.BadRequest, response.status)
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
        assertEquals(HttpStatusCode.BadRequest, response.status)
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
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
