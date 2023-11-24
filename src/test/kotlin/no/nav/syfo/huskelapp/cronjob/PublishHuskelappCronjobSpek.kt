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
import java.time.LocalDate
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

        it("publishes unpublished huskelapper oldest first") {
            val enHuskelapp = Huskelapp.create(
                tekst = "En huskelapp",
                personIdent = personIdent,
                veilederIdent = veilederIdent,
            )
            val annenHuskelapp = Huskelapp.create(
                tekst = "Annen huskelapp",
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                frist = LocalDate.now().plusWeeks(1)
            )
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

            val enKafkaHuskelapp = kafkaRecordSlot1.captured.value()

            enKafkaHuskelapp.tekst shouldBeEqualTo enHuskelapp.tekst
            enKafkaHuskelapp.personIdent shouldBeEqualTo enHuskelapp.personIdent.value
            enKafkaHuskelapp.veilederIdent shouldBeEqualTo enHuskelapp.createdBy
            enKafkaHuskelapp.isActive shouldBeEqualTo enHuskelapp.isActive
            enKafkaHuskelapp.frist.shouldBeNull()

            val annenKafkaHuskelapp = kafkaRecordSlot2.captured.value()
            annenKafkaHuskelapp.frist shouldBeEqualTo annenHuskelapp.frist

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
