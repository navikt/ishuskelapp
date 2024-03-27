package no.nav.syfo.testhelper

import io.ktor.server.application.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.application.HuskelappService
import no.nav.syfo.infrastructure.database.repository.HuskelappRepository

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
