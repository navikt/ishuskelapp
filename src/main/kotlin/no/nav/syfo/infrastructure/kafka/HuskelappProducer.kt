package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.util.*

class HuskelappProducer(
    private val kafkaProducer: KafkaProducer<String, KafkaHuskelapp>,
) {
    fun sendHuskelapp(huskelapp: Oppfolgingsoppgave) {
        val kafkaHuskelapp = KafkaHuskelapp(
            uuid = huskelapp.uuid,
            personIdent = huskelapp.personIdent.value,
            veilederIdent = huskelapp.createdBy,
            tekst = huskelapp.tekst,
            oppfolgingsgrunner = huskelapp.oppfolgingsgrunner,
            isActive = huskelapp.isActive,
            createdAt = huskelapp.createdAt,
            updatedAt = huskelapp.updatedAt,
            frist = huskelapp.frist,
        )
        val key = UUID.nameUUIDFromBytes(kafkaHuskelapp.personIdent.toByteArray()).toString()
        try {
            kafkaProducer.send(
                ProducerRecord(
                    HUSKELAPP_TOPIC,
                    key,
                    kafkaHuskelapp,
                )
            ).get()
            log.info("KafkaHuskelapp with uuid ${kafkaHuskelapp.uuid} sent to huskelapp topic")
        } catch (e: Exception) {
            log.error(
                "Exception was thrown when attempting to send KafkaHuskelapp with key {}: ${e.message}",
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

fun huskelappKafkaProducer(kafkaEnvironment: KafkaEnvironment) =
    KafkaProducer<String, KafkaHuskelapp>(
        kafkaAivenProducerConfig<KafkaHuskelappSerializer>(kafkaEnvironment = kafkaEnvironment)
    )

class KafkaHuskelappSerializer : Serializer<KafkaHuskelapp> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KafkaHuskelapp?): ByteArray =
        mapper.writeValueAsBytes(data)
}
