package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.infrastructure.cronjob.cronjobModule
import no.nav.syfo.infrastructure.database.applicationDatabase
import no.nav.syfo.infrastructure.database.databaseModule
import no.nav.syfo.infrastructure.client.azuread.AzureAdClient
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.client.wellknown.getWellKnown
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.infrastructure.database.repository.HuskelappRepository
import no.nav.syfo.infrastructure.kafka.HuskelappProducer
import no.nav.syfo.infrastructure.kafka.huskelappKafkaProducer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState()
    val logger = LoggerFactory.getLogger("ktor.application")
    val environment = Environment()
    val wellKnownInternalAzureAD = getWellKnown(
        wellKnownUrl = environment.azure.appWellKnownUrl,
    )
    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure
    )
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.istilgangskontroll,
    )
    val huskelappProducer = HuskelappProducer(
        kafkaProducer = huskelappKafkaProducer(
            kafkaEnvironment = environment.kafka,
        )
    )

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = logger
        config = HoconApplicationConfig(ConfigFactory.load())
        connector {
            port = applicationPort
        }
        module {
            databaseModule(
                databaseEnvironment = environment.database,
            )
            val huskelappRepository = HuskelappRepository(
                database = applicationDatabase,
            )
            val huskelappService = HuskelappService(
                huskelappRepository = huskelappRepository,
            )
            apiModule(
                applicationState = applicationState,
                database = applicationDatabase,
                environment = environment,
                wellKnownInternalAzureAD = wellKnownInternalAzureAD,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
                huskelappService = huskelappService,
            )
            cronjobModule(
                applicationState = applicationState,
                environment = environment,
                huskelappService = huskelappService,
                huskelappProducer = huskelappProducer,
            )
        }
    }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        applicationState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")
    }

    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment,
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = true)
}
