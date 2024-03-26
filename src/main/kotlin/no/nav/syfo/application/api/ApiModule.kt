package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.auth.JwtIssuer
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.api.auth.installJwtAuthentication
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.application.metric.registerMetricApi
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.client.wellknown.WellKnown
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.huskelapp.api.registerHuskelappApi

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownInternalAzureAD: WellKnown,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    huskelappService: HuskelappService,
) {
    installMetrics()
    installCallId()
    installContentNegotiation()
    installStatusPages()
    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.azure.appClientId),
                jwtIssuerType = JwtIssuerType.INTERNAL_AZUREAD,
                wellKnown = wellKnownInternalAzureAD,
            ),
        )
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerMetricApi()
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
            registerHuskelappApi(
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
                huskelappService = huskelappService,
            )
        }
    }
}
