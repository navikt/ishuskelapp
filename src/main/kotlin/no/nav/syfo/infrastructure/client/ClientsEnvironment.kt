package no.nav.syfo.infrastructure.client

data class ClientsEnvironment(
    val istilgangskontroll: ClientEnvironment,
)

data class ClientEnvironment(
    val baseUrl: String,
    val clientId: String,
)
