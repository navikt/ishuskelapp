package no.nav.syfo.testhelper

import no.nav.syfo.ApplicationState
import no.nav.syfo.infrastructure.client.azuread.AzureAdClient
import no.nav.syfo.infrastructure.client.wellknown.WellKnown
import no.nav.syfo.testhelper.mock.mockHttpClient
import java.nio.file.Paths

fun wellKnownInternalAzureAD(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString()
    )
}

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        httpClient = mockHttpClient,
    )

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
