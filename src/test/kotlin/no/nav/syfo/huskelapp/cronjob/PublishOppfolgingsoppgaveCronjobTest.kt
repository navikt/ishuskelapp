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
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.util.concurrent.Future
import kotlin.test.assertNull

class PublishOppfolgingsoppgaveCronjobTest {

    private val personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT
    private val veilederIdent = UserConstants.VEILEDER_IDENT

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaProducer = mockk<KafkaProducer<String, OppfolgingsoppgaveRecord>>()

    private val oppfolgingsoppgaveProducer = OppfolgingsoppgaveProducer(
        producer = kafkaProducer,
    )
    private val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(
        database = database,
    )
    private val oppfolgingsoppgaveService = OppfolgingsoppgaveService(
        oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
    )
    private val publishOppfolgingsoppgaveCronjob = PublishOppfolgingsoppgaveCronjob(
        oppfolgingsoppgaveService = oppfolgingsoppgaveService,
        oppfolgingsoppgaveProducer = oppfolgingsoppgaveProducer,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(kafkaProducer)
        coEvery {
            kafkaProducer.send(any())
        } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        database.dropData()
    }

    @Test
    fun `publishes unpublished oppfolgingsoppgaver oldest first`() {
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

        assertEquals(0, result.failed)
        assertEquals(2, result.updated)

        val kafkaRecordSlot1 = slot<ProducerRecord<String, OppfolgingsoppgaveRecord>>()
        val kafkaRecordSlot2 = slot<ProducerRecord<String, OppfolgingsoppgaveRecord>>()
        verifyOrder {
            kafkaProducer.send(capture(kafkaRecordSlot1))
            kafkaProducer.send(capture(kafkaRecordSlot2))
        }

        val enOppfolgingsoppgaveRecord = kafkaRecordSlot1.captured.value()

        assertEquals(enOppfolgingsoppgave.sisteVersjon().tekst, enOppfolgingsoppgaveRecord.tekst)
        assertEquals(listOf(enOppfolgingsoppgave.sisteVersjon().oppfolgingsgrunn), enOppfolgingsoppgaveRecord.oppfolgingsgrunner)
        assertEquals(enOppfolgingsoppgave.personIdent.value, enOppfolgingsoppgaveRecord.personIdent)
        assertEquals(enOppfolgingsoppgave.sisteVersjon().createdBy, enOppfolgingsoppgaveRecord.veilederIdent)
        assertEquals(enOppfolgingsoppgave.isActive, enOppfolgingsoppgaveRecord.isActive)
        assertNull(enOppfolgingsoppgaveRecord.frist)

        val annenOppfolgingsoppgaveRecord = kafkaRecordSlot2.captured.value()
        assertEquals(annenOppfolgingsoppgave.sisteVersjon().frist, annenOppfolgingsoppgaveRecord.frist)

        assertTrue(
            oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(personIdent)
                .all { it.publishedAt != null }
        )
    }

    @Test
    fun `does not publish published huskelapp`() {
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

        assertEquals(0, result.failed)
        assertEquals(0, result.updated)

        verify(exactly = 0) {
            kafkaProducer.send(any())
        }
    }

    @Test
    fun `publishes nothing if no huskelapper`() {
        val result = publishOppfolgingsoppgaveCronjob.runJob()

        assertEquals(0, result.failed)
        assertEquals(0, result.updated)

        verify(exactly = 0) {
            kafkaProducer.send(any())
        }
    }

    @Test
    fun `publishes edited huskelapp`() {
        val oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
        val enHuskelapp = Oppfolgingsoppgave.create(
            personIdent,
            veilederIdent,
            tekst = "En oppfolgingsoppgave",
            oppfolgingsgrunn = oppfolgingsgrunn
        )
        oppfolgingsoppgaveRepository.create(enHuskelapp)

        var result = publishOppfolgingsoppgaveCronjob.runJob()

        assertEquals(0, result.failed)
        assertEquals(1, result.updated)

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

        assertEquals(0, result.failed)
        assertEquals(1, result.updated)

        val kafkaRecordSlot = slot<ProducerRecord<String, OppfolgingsoppgaveRecord>>()
        verifyOrder {
            kafkaProducer.send(capture(kafkaRecordSlot))
        }

        val kafkaHuskelapp = kafkaRecordSlot.captured.value()

        assertEquals(newTekst, kafkaHuskelapp.tekst)
        assertEquals(newFrist, kafkaHuskelapp.frist)
        assertEquals(listOf(enHuskelapp.sisteVersjon().oppfolgingsgrunn), kafkaHuskelapp.oppfolgingsgrunner)
        assertEquals(enHuskelapp.personIdent.value, kafkaHuskelapp.personIdent)
        assertEquals(enHuskelapp.sisteVersjon().createdBy, kafkaHuskelapp.veilederIdent)
        assertEquals(enHuskelapp.isActive, kafkaHuskelapp.isActive)

        assertTrue(
            oppfolgingsoppgaveRepository.getPOppfolgingsoppgaver(personIdent)
                .all { it.publishedAt != null }
        )
    }
}
