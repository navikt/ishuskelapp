package no.nav.syfo.testhelper

import io.ktor.server.application.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = externalMockEnvironment.azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.istilgangskontroll,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    val huskelappRepository = OppfolgingsoppgaveRepository(
        database = externalMockEnvironment.database,
    )
    val huskelappService = OppfolgingsoppgaveService(
        oppfolgingsoppgaveRepository = huskelappRepository,
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
