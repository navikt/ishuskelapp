package no.nav.syfo.infrastructure.cronjob

import io.ktor.server.application.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.infrastructure.client.leaderelection.LeaderPodClient
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.infrastructure.kafka.OppfolgingsoppgaveProducer
import no.nav.syfo.launchBackgroundTask

fun Application.cronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
    oppfolgingsoppgaveService: OppfolgingsoppgaveService,
    oppfolgingsoppgaveProducer: OppfolgingsoppgaveProducer,
) {
    val leaderPodClient = LeaderPodClient(
        electorPath = environment.electorPath
    )
    val cronjobRunner = CronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient
    )
    val publishOppfolgingsoppgaveCronjob = PublishOppfolgingsoppgaveCronjob(
        oppfolgingsoppgaveService = oppfolgingsoppgaveService,
        oppfolgingsoppgaveProducer = oppfolgingsoppgaveProducer,
    )

    val allCronjobs = mutableListOf(publishOppfolgingsoppgaveCronjob)
    allCronjobs.forEach {
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(cronjob = it)
        }
    }
}
