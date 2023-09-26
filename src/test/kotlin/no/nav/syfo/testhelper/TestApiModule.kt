package no.nav.syfo.testhelper

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.huskelapp.database.HuskelappRepository

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = externalMockEnvironment.azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.istilgangskontroll,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    val huskelappRepository = HuskelappRepository(
        database = externalMockEnvironment.database,
    )
    val huskelappService = HuskelappService(
        huskelappRepository = huskelappRepository,
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        huskelappService = huskelappService,
    )
}
