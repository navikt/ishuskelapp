package no.nav.syfo.huskelapp.cronjob

import io.mockk.*
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.huskelapp.database.HuskelappRepository
import no.nav.syfo.huskelapp.domain.Huskelapp
import no.nav.syfo.huskelapp.kafka.HuskelappProducer
import no.nav.syfo.huskelapp.kafka.KafkaHuskelapp
import no.nav.syfo.testhelper.*
import org.amshove.kluent.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.Future

class PublishHuskelappCronjobSpek : Spek({

    val personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT
    val veilederIdent = UserConstants.VEILEDER_IDENT

    describe(PublishHuskelappCronjob::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val kafkaProducer = mockk<KafkaProducer<String, KafkaHuskelapp>>()

        val huskelappProducer = HuskelappProducer(
            kafkaProducer = kafkaProducer,
        )
        val huskelappRepository = HuskelappRepository(
            database = database,
        )
        val huskelappService = HuskelappService(
            huskelappRepository = huskelappRepository,
        )
        val publishHuskelappCronjob = PublishHuskelappCronjob(
            huskelappService = huskelappService,
            huskelappProducer = huskelappProducer,
        )

        beforeEachTest {
            clearMocks(kafkaProducer)
            coEvery {
                kafkaProducer.send(any())
            } returns mockk<Future<RecordMetadata>>(relaxed = true)
        }
        afterEachTest {
            database.dropData()
        }

        it("publishes unpublished huskelapper") {
            val enHuskelapp = Huskelapp.create("En huskelapp", personIdent, veilederIdent)
            val annenHuskelapp = Huskelapp.create("Annen huskelapp", personIdent, veilederIdent)
            listOf(enHuskelapp, annenHuskelapp).forEach {
                huskelappRepository.create(it)
            }

            val result = publishHuskelappCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 2

            val kafkaRecordSlot1 = slot<ProducerRecord<String, KafkaHuskelapp>>()
            val kafkaRecordSlot2 = slot<ProducerRecord<String, KafkaHuskelapp>>()
            verifyOrder {
                kafkaProducer.send(capture(kafkaRecordSlot1))
                kafkaProducer.send(capture(kafkaRecordSlot2))
            }

            val enKafkaHuskelapp = listOf(
                kafkaRecordSlot1.captured.value(),
                kafkaRecordSlot2.captured.value()
            ).first { it.uuid == enHuskelapp.uuid }

            enKafkaHuskelapp.tekst shouldBeEqualTo enHuskelapp.tekst
            enKafkaHuskelapp.personIdent shouldBeEqualTo enHuskelapp.personIdent.value
            enKafkaHuskelapp.veilederIdent shouldBeEqualTo enHuskelapp.veilederIdent
            enKafkaHuskelapp.isActive shouldBeEqualTo enHuskelapp.isActive

            huskelappRepository.getHuskelapper(personIdent).all { it.publishedAt != null } shouldBeEqualTo true
        }
        it("does not publish published huskelapp") {
            val enHuskelapp = Huskelapp.create("En huskelapp", personIdent, veilederIdent)
            huskelappRepository.create(enHuskelapp)
            huskelappRepository.setPublished(enHuskelapp)

            val result = publishHuskelappCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 0

            verify(exactly = 0) {
                kafkaProducer.send(any())
            }
        }
        it("publishes nothing if no huskelapper") {
            val result = publishHuskelappCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 0

            verify(exactly = 0) {
                kafkaProducer.send(any())
            }
        }
    }
})