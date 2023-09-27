package no.nav.syfo.huskelapp.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.testhelper.*
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

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

            afterEachTest {
                database.dropData()
            }

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = UserConstants.VEILEDER_IDENT,
            )

            describe("Get huskelapp for person") {
                describe("Happy path") {
                    it("OK") {
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

                describe("Post huskelapp") {
                    describe("Happy path") {
                        val requestDTO = HuskelappRequestDTO(
                            tekst = "Hei",
                        )
                        it("OK") {
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
                                handleRequest(HttpMethod.Get, huskelappApiBasePath) {
                                    addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                    addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                                }
                            ) {
                                response.status() shouldBeEqualTo HttpStatusCode.OK
                                val responseDTO =
                                    objectMapper.readValue<HuskelappResponseDTO>(response.content!!)

                                responseDTO.tekst shouldBeEqualTo requestDTO.tekst
                                responseDTO.veilederIdent shouldBeEqualTo UserConstants.VEILEDER_IDENT
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
