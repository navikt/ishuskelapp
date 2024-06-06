package no.nav.syfo.testhelper.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import no.nav.syfo.infrastructure.client.azuread.AzureAdTokenResponse

fun azureAdMockResponse(token: String?) =
    AzureAdTokenResponse(
        access_token = token ?: "token",
        expires_in = 3600,
        token_type = "type",
    )

fun MockRequestHandleScope.getAzureAdResponse(request: HttpRequestData): HttpResponseData {
    val token = (request.body as FormDataContent).formData["assertion"]
    return respond(
        content = mapper.writeValueAsString(azureAdMockResponse(token)),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}
