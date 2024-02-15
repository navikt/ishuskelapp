package no.nav.syfo.huskelapp.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.cronjob.Cronjob
import no.nav.syfo.application.cronjob.CronjobResult
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.huskelapp.kafka.HuskelappProducer
import org.slf4j.LoggerFactory

class PublishHuskelappCronjob(
    private val huskelappService: HuskelappService,
    private val huskelappProducer: HuskelappProducer
) : Cronjob {
    override val initialDelayMinutes: Long = 2
    override val intervalDelaySeconds: Long = 20

    override suspend fun run() {
        val result = runJob()
        log.info(
            "Completed publishing huskelapp processing job with result: {}, {}",
            StructuredArguments.keyValue("failed", result.failed),
            StructuredArguments.keyValue("updated", result.updated),
        )
    }

    fun runJob(): CronjobResult {
        val result = CronjobResult()
        val unpublishedHuskelapper = huskelappService.getUnpublishedHuskelapper()

        unpublishedHuskelapper.forEach {
            try {
                huskelappProducer.sendHuskelapp(it)
                huskelappService.setPublished(it)
                result.updated++
            } catch (e: Exception) {
                log.error("Caught exception in publish huskelapp job", e)
                result.failed++
            }
        }

        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
