package no.nav.syfo.application.cronjob

import io.ktor.server.application.*
import no.nav.syfo.application.*
import no.nav.syfo.client.leaderelection.LeaderPodClient
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.huskelapp.cronjob.PublishHuskelappCronjob
import no.nav.syfo.huskelapp.kafka.HuskelappProducer

fun Application.cronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
    huskelappService: HuskelappService,
    huskelappProducer: HuskelappProducer,
) {
    val leaderPodClient = LeaderPodClient(
        electorPath = environment.electorPath
    )
    val cronjobRunner = CronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient
    )
    val publishHuskelappCronjob = PublishHuskelappCronjob(
        huskelappService = huskelappService,
        huskelappProducer = huskelappProducer,
    )

    val allCronjobs = mutableListOf(publishHuskelappCronjob)
    allCronjobs.forEach {
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(cronjob = it)
        }
    }
}
