package no.nav.syfo.huskelapp.cronjob

import io.mockk.*
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.application.EditedOppfolgingsoppgaveDTO
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.infrastructure.cronjob.PublishOppfolgingsoppgaveCronjob
import no.nav.syfo.infrastructure.kafka.HuskelappProducer
import no.nav.syfo.infrastructure.kafka.KafkaHuskelapp
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

    describe(PublishOppfolgingsoppgaveCronjob::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val kafkaProducer = mockk<KafkaProducer<String, KafkaHuskelapp>>()

        val huskelappProducer = HuskelappProducer(
            kafkaProducer = kafkaProducer,
        )
        val huskelappRepository = OppfolgingsoppgaveRepository(
            database = database,
        )
        val huskelappService = OppfolgingsoppgaveService(
            oppfolgingsoppgaveRepository = huskelappRepository,
        )
        val publishOppfolgingsoppgaveCronjob = PublishOppfolgingsoppgaveCronjob(
            oppfolgingsoppgaveService = huskelappService,
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
            val enHuskelapp = Oppfolgingsoppgave.create(
                personIdent,
                veilederIdent,
                tekst = "En huskelapp",
                oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
            )
            val annenHuskelapp = Oppfolgingsoppgave.create(
                personIdent,
                veilederIdent,
                tekst = null,
                oppfolgingsgrunner = listOf(Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT),
                frist = LocalDate.now().plusWeeks(1),
            )
            listOf(enHuskelapp, annenHuskelapp).forEach {
                huskelappRepository.create(it)
            }

            val result = publishOppfolgingsoppgaveCronjob.runJob()

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
            enKafkaHuskelapp.oppfolgingsgrunner shouldBeEqualTo enHuskelapp.oppfolgingsgrunner
            enKafkaHuskelapp.personIdent shouldBeEqualTo enHuskelapp.personIdent.value
            enKafkaHuskelapp.veilederIdent shouldBeEqualTo enHuskelapp.createdBy
            enKafkaHuskelapp.isActive shouldBeEqualTo enHuskelapp.isActive
            enKafkaHuskelapp.frist.shouldBeNull()

            val annenKafkaHuskelapp = kafkaRecordSlot2.captured.value()
            annenKafkaHuskelapp.frist shouldBeEqualTo annenHuskelapp.frist

            huskelappRepository.getOppfolgingsoppgaver(personIdent).all { it.publishedAt != null } shouldBeEqualTo true
        }
        it("does not publish published huskelapp") {
            val enHuskelapp = Oppfolgingsoppgave.create(
                personIdent = personIdent,
                veilederIdent = veilederIdent,
                tekst = "En huskelapp",
                oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
            )
            huskelappRepository.create(enHuskelapp)
            val publishedHuskelapp = enHuskelapp.publish()
            huskelappRepository.updatePublished(publishedHuskelapp)

            val result = publishOppfolgingsoppgaveCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 0

            verify(exactly = 0) {
                kafkaProducer.send(any())
            }
        }
        it("publishes nothing if no huskelapper") {
            val result = publishOppfolgingsoppgaveCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 0

            verify(exactly = 0) {
                kafkaProducer.send(any())
            }
        }
        it("publishes edited huskelapp") {
            val enHuskelapp = Oppfolgingsoppgave.create(
                personIdent,
                veilederIdent,
                tekst = "En huskelapp",
                oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
            )
            huskelappRepository.create(enHuskelapp)

            var result = publishOppfolgingsoppgaveCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 1

            verify(exactly = 1) {
                kafkaProducer.send(any())
            }

            val editedOppfolgingsoppgaveDTO =
                EditedOppfolgingsoppgaveDTO(tekst = "En huskelapp", frist = LocalDate.now().plusDays(3))
            huskelappService.addVersion(
                existingOppfolgingsoppgaveUuid = enHuskelapp.uuid,
                newVersion = editedOppfolgingsoppgaveDTO
            )

            result = publishOppfolgingsoppgaveCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 1

            val kafkaRecordSlot = slot<ProducerRecord<String, KafkaHuskelapp>>()
            verifyOrder {
                kafkaProducer.send(capture(kafkaRecordSlot))
            }

            val kafkaHuskelapp = kafkaRecordSlot.captured.value()

            kafkaHuskelapp.tekst shouldBeEqualTo editedOppfolgingsoppgaveDTO.tekst
            kafkaHuskelapp.frist shouldBeEqualTo editedOppfolgingsoppgaveDTO.frist
            kafkaHuskelapp.oppfolgingsgrunner shouldBeEqualTo enHuskelapp.oppfolgingsgrunner
            kafkaHuskelapp.personIdent shouldBeEqualTo enHuskelapp.personIdent.value
            kafkaHuskelapp.veilederIdent shouldBeEqualTo enHuskelapp.createdBy
            kafkaHuskelapp.isActive shouldBeEqualTo enHuskelapp.isActive

            huskelappRepository.getOppfolgingsoppgaver(personIdent).all { it.publishedAt != null } shouldBeEqualTo true
        }
    }
})
