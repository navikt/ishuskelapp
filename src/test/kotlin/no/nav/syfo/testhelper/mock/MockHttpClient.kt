package no.nav.syfo.testhelper.mock

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import no.nav.syfo.Environment
import no.nav.syfo.infrastructure.client.commonConfig

fun mockHttpClient(environment: Environment) = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler { request ->
            val requestUrl = request.url.encodedPath
            when {
                requestUrl == "/${environment.azure.openidConfigTokenEndpoint}" -> getAzureAdResponse(request)
                requestUrl.startsWith("/${environment.clients.istilgangskontroll.baseUrl}") -> isTilgangskontrollResponse(
                    request
                )

                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
    }
}
