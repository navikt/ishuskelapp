package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.util.*

class OppfolgingsoppgaveProducer(
    private val producer: KafkaProducer<String, OppfolgingsoppgaveRecord>,
) {
    fun send(oppfolgingsoppgave: Oppfolgingsoppgave) {
        val sisteVersjon = oppfolgingsoppgave.sisteVersjon()
        val oppfolgingsoppgaveRecord = OppfolgingsoppgaveRecord(
            uuid = oppfolgingsoppgave.uuid,
            personIdent = oppfolgingsoppgave.personIdent.value,
            veilederIdent = sisteVersjon.createdBy,
            tekst = sisteVersjon.tekst,
            oppfolgingsgrunner = listOf(sisteVersjon.oppfolgingsgrunn),
            isActive = oppfolgingsoppgave.isActive,
            createdAt = oppfolgingsoppgave.createdAt,
            updatedAt = oppfolgingsoppgave.updatedAt,
            frist = sisteVersjon.frist,
        )
        val key = UUID.nameUUIDFromBytes(oppfolgingsoppgaveRecord.personIdent.toByteArray()).toString()
        try {
            producer.send(
                ProducerRecord(
                    HUSKELAPP_TOPIC,
                    key,
                    oppfolgingsoppgaveRecord,
                )
            ).get()
            log.info("OppfolgingsoppgaveRecord with uuid ${oppfolgingsoppgaveRecord.uuid} sent to huskelapp topic")
        } catch (e: Exception) {
            log.error(
                "Exception was thrown when attempting to send OppfolgingsoppgaveRecord with key {}: ${e.message}",
                key
            )
            throw e
        }
    }

    companion object {
        const val HUSKELAPP_TOPIC = "teamsykefravr.huskelapp"
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}

fun oppfolgingsoppgaveKafkaProducer(kafkaEnvironment: KafkaEnvironment) =
    KafkaProducer<String, OppfolgingsoppgaveRecord>(
        kafkaAivenProducerConfig<OppfolgingsoppgaveSerializer>(kafkaEnvironment = kafkaEnvironment)
    )

class OppfolgingsoppgaveSerializer : Serializer<OppfolgingsoppgaveRecord> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: OppfolgingsoppgaveRecord?): ByteArray =
        mapper.writeValueAsBytes(data)
}
