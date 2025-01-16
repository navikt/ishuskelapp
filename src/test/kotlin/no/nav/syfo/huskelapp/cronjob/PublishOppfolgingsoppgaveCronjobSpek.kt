package no.nav.syfo.huskelapp.cronjob

import io.mockk.*
import no.nav.syfo.application.OppfolgingsoppgaveService
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.infrastructure.cronjob.PublishOppfolgingsoppgaveCronjob
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.infrastructure.kafka.OppfolgingsoppgaveProducer
import no.nav.syfo.infrastructure.kafka.OppfolgingsoppgaveRecord
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.util.concurrent.Future

class PublishOppfolgingsoppgaveCronjobSpek : Spek({

    val personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT
    val veilederIdent = UserConstants.VEILEDER_IDENT

    describe(PublishOppfolgingsoppgaveCronjob::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val kafkaProducer = mockk<KafkaProducer<String, OppfolgingsoppgaveRecord>>()

        val oppfolgingsoppgaveProducer = OppfolgingsoppgaveProducer(
            producer = kafkaProducer,
        )
        val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(
            database = database,
        )
        val oppfolgingsoppgaveService = OppfolgingsoppgaveService(
            oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
        )
        val publishOppfolgingsoppgaveCronjob = PublishOppfolgingsoppgaveCronjob(
            oppfolgingsoppgaveService = oppfolgingsoppgaveService,
            oppfolgingsoppgaveProducer = oppfolgingsoppgaveProducer,
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

        it("publishes unpublished oppfolgingsoppgaver oldest first") {
            val enOppfolgingsoppgave = Oppfolgingsoppgave.create(
                personIdent,
                veilederIdent,
                tekst = "En oppfolgingsoppgave",
                oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
            )
            val annenOppfolgingsoppgave = Oppfolgingsoppgave.create(
                personIdent,
                veilederIdent,
                tekst = null,
                oppfolgingsgrunn = Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT,
                frist = LocalDate.now().plusWeeks(1),
            )
            listOf(enOppfolgingsoppgave, annenOppfolgingsoppgave).forEach {
                oppfolgingsoppgaveRepository.create(it)
            }

            val result = publishOppfolgingsoppgaveCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 2

            val kafkaRecordSlot1 = slot<ProducerRecord<String, OppfolgingsoppgaveRecord>>()
            val kafkaRecordSlot2 = slot<ProducerRecord<String, OppfolgingsoppgaveRecord>>()
            verifyOrder {
                kafkaProducer.send(capture(kafkaRecordSlot1))
                kafkaProducer.send(capture(kafkaRecordSlot2))
            }

            val enOppfolgingsoppgaveRecord = kafkaRecordSlot1.captured.value()

            enOppfolgingsoppgaveRecord.tekst shouldBeEqualTo enOppfolgingsoppgave.sisteVersjon().tekst
            enOppfolgingsoppgaveRecord.oppfolgingsgrunner shouldBeEqualTo listOf(enOppfolgingsoppgave.sisteVersjon().oppfolgingsgrunn)
            enOppfolgingsoppgaveRecord.personIdent shouldBeEqualTo enOppfolgingsoppgave.personIdent.value
            enOppfolgingsoppgaveRecord.veilederIdent shouldBeEqualTo enOppfolgingsoppgave.sisteVersjon().createdBy
            enOppfolgingsoppgaveRecord.isActive shouldBeEqualTo enOppfolgingsoppgave.isActive
            enOppfolgingsoppgaveRecord.frist.shouldBeNull()

            val annenOppfolgingsoppgaveRecord = kafkaRecordSlot2.captured.value()
            annenOppfolgingsoppgaveRecord.frist shouldBeEqualTo annenOppfolgingsoppgave.sisteVersjon().frist

            oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(personIdent)
                .all { it.publishedAt != null } shouldBeEqualTo true
        }
        it("does not publish published huskelapp") {
            val enHuskelapp: Oppfolgingsoppgave =
                Oppfolgingsoppgave.create(
                    personIdent = personIdent,
                    veilederIdent = veilederIdent,
                    tekst = "En oppfolgingsoppgave",
                    oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
                )

            oppfolgingsoppgaveRepository.create(enHuskelapp)
            val publishedHuskelapp = enHuskelapp.publish()
            oppfolgingsoppgaveRepository.updatePublished(publishedHuskelapp)

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
            val oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
            val enHuskelapp = Oppfolgingsoppgave.create(
                personIdent,
                veilederIdent,
                tekst = "En oppfolgingsoppgave",
                oppfolgingsgrunn = oppfolgingsgrunn
            )
            oppfolgingsoppgaveRepository.create(enHuskelapp)

            var result = publishOppfolgingsoppgaveCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 1

            verify(exactly = 1) {
                kafkaProducer.send(any())
            }

            val newTekst = "En oppfolgingsoppgave"
            val newFrist = LocalDate.now().plusDays(3)
            oppfolgingsoppgaveService.editOppfolgingsoppgave(
                existingOppfolgingsoppgaveUuid = enHuskelapp.uuid,
                veilederIdent = veilederIdent,
                newOppfolgingsgrunn = oppfolgingsgrunn,
                newTekst = newTekst,
                newFrist = newFrist,
            )

            result = publishOppfolgingsoppgaveCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 1

            val kafkaRecordSlot = slot<ProducerRecord<String, OppfolgingsoppgaveRecord>>()
            verifyOrder {
                kafkaProducer.send(capture(kafkaRecordSlot))
            }

            val kafkaHuskelapp = kafkaRecordSlot.captured.value()

            kafkaHuskelapp.tekst shouldBeEqualTo newTekst
            kafkaHuskelapp.frist shouldBeEqualTo newFrist
            kafkaHuskelapp.oppfolgingsgrunner shouldBeEqualTo listOf(enHuskelapp.sisteVersjon().oppfolgingsgrunn)
            kafkaHuskelapp.personIdent shouldBeEqualTo enHuskelapp.personIdent.value
            kafkaHuskelapp.veilederIdent shouldBeEqualTo enHuskelapp.sisteVersjon().createdBy
            kafkaHuskelapp.isActive shouldBeEqualTo enHuskelapp.isActive

            oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(personIdent)
                .all { it.publishedAt != null } shouldBeEqualTo true
        }
    }
})
