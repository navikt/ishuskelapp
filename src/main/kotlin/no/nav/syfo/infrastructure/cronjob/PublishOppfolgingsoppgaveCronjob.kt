package no.nav.syfo.infrastructure.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.infrastructure.kafka.OppfolgingsoppgaveProducer
import org.slf4j.LoggerFactory

class PublishOppfolgingsoppgaveCronjob(
    private val oppfolgingsoppgaveService: OppfolgingsoppgaveService,
    private val oppfolgingsoppgaveProducer: OppfolgingsoppgaveProducer
) : Cronjob {
    override val initialDelayMinutes: Long = 2
    override val intervalDelaySeconds: Long = 20

    override suspend fun run() {
        val result = runJob()
        log.info(
            "Completed publishing oppfolgingsoppgave processing job with result: {}, {}",
            StructuredArguments.keyValue("failed", result.failed),
            StructuredArguments.keyValue("updated", result.updated),
        )
    }

    fun runJob(): CronjobResult {
        val result = CronjobResult()
        val unpublishedOppfolgingsoppgaver = oppfolgingsoppgaveService.getUnpublishedOppfolgingsoppgaver()

        unpublishedOppfolgingsoppgaver.forEach {
            try {
                oppfolgingsoppgaveProducer.send(it)
                val publishedOppfolgingsoppgave = it.publish()
                oppfolgingsoppgaveService.updatePublished(publishedOppfolgingsoppgave)
                result.updated++
            } catch (e: Exception) {
                log.error("Caught exception in publish oppfolgingsoppgave job", e)
                result.failed++
            }
        }

        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
