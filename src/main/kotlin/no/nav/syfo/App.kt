package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.infrastructure.client.azuread.AzureAdClient
import no.nav.syfo.infrastructure.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.client.wellknown.getWellKnown
import no.nav.syfo.infrastructure.cronjob.cronjobModule
import no.nav.syfo.infrastructure.database.applicationDatabase
import no.nav.syfo.infrastructure.database.databaseModule
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.infrastructure.kafka.OppfolgingsoppgaveProducer
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseConsumer
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseService
import no.nav.syfo.infrastructure.kafka.identhendelse.launchIdenthendelseConsumer
import no.nav.syfo.infrastructure.kafka.oppfolgingsoppgaveKafkaProducer
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
    val oppfolgingsoppgaveProducer = OppfolgingsoppgaveProducer(
        producer = oppfolgingsoppgaveKafkaProducer(
            kafkaEnvironment = environment.kafka,
        )
    )

    val applicationEngineEnvironment = applicationEnvironment {
        log = logger
        config = HoconApplicationConfig(ConfigFactory.load())
    }
    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment,
        configure = {
            connector {
                port = applicationPort
            }
        },
        module = {
            databaseModule(
                databaseEnvironment = environment.database,
            )
            val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(
                database = applicationDatabase,
            )
            val oppfolgingsoppgaveService = OppfolgingsoppgaveService(
                oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
            )
            apiModule(
                applicationState = applicationState,
                database = applicationDatabase,
                environment = environment,
                wellKnownInternalAzureAD = wellKnownInternalAzureAD,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
                oppfolgingsoppgaveService = oppfolgingsoppgaveService,
            )
            cronjobModule(
                applicationState = applicationState,
                environment = environment,
                oppfolgingsoppgaveService = oppfolgingsoppgaveService,
                oppfolgingsoppgaveProducer = oppfolgingsoppgaveProducer,
            )
            launchIdenthendelseConsumer(
                applicationState = applicationState,
                kafkaEnvironment = environment.kafka,
                identhendelseConsumer = IdenthendelseConsumer(
                    identhendelseService = IdenthendelseService(
                        oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
                    )
                ),
            )

            monitor.subscribe(ApplicationStarted) {
                applicationState.ready = true
                logger.info("Application is ready, running Java VM ${Runtime.version()}")
            }
        }
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = true)
}

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = false
)
