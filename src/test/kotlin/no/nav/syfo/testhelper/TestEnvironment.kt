package no.nav.syfo.testhelper

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.infrastructure.database.DatabaseEnvironment
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.client.ClientEnvironment
import no.nav.syfo.infrastructure.client.ClientsEnvironment
import no.nav.syfo.infrastructure.client.azuread.AzureEnvironment

fun testEnvironment() = Environment(
    database = DatabaseEnvironment(
        host = "localhost",
        port = "5432",
        name = "ishuskelapp_dev",
        username = "username",
        password = "password",
    ),
    azure = AzureEnvironment(
        appClientId = "ishuskelapp-client-id",
        appClientSecret = "ishuskelapp-secret",
        appWellKnownUrl = "wellknown",
        openidConfigTokenEndpoint = "azureOpenIdTokenEndpoint",
    ),
    kafka = KafkaEnvironment(
        aivenBootstrapServers = "kafkaBootstrapServers",
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    electorPath = "electorPath",
    clients = ClientsEnvironment(
        istilgangskontroll = ClientEnvironment(
            baseUrl = "istilgangskontrollUrl",
            clientId = "dev-gcp.teamsykefravr.istilgangskontroll",
        ),
    ),
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
