package no.nav.syfo.application

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT
import no.nav.syfo.testhelper.dropData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppfolgingsoppgaveServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(database = database)
    private val oppfolgingsoppgaveService = OppfolgingsoppgaveService(oppfolgingsoppgaveRepository)

    private val oppfolgingsoppgave: Oppfolgingsoppgave =
        Oppfolgingsoppgave.create(
            personIdent = ARBEIDSTAKER_PERSONIDENT,
            veilederIdent = VEILEDER_IDENT,
            tekst = "En god tekst for en oppfolgingsoppgave",
            oppfolgingsgrunn = Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT,
            frist = LocalDate.now().plusDays(1),
        )

    @AfterEach
    fun tearDown() {
        database.dropData()
    }

    @Test
    fun `adds a new version of oppfolgingsoppgave with only edited frist`() {
        val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
        val publishedOppfolgingsoppgave = createdOppfolgingsoppgave.publish()
        oppfolgingsoppgaveRepository.updatePublished(publishedOppfolgingsoppgave)

        val sisteVersjon = oppfolgingsoppgave.sisteVersjon()
        val newFrist = sisteVersjon.frist!!.plusWeeks(1)
        val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
            existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
            veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
            newOppfolgingsgrunn = sisteVersjon.oppfolgingsgrunn,
            newTekst = sisteVersjon.tekst,
            newFrist = newFrist,
        )

        assertEquals(oppfolgingsoppgave.uuid, newOppfolgingsoppgave?.uuid)
        assertEquals(oppfolgingsoppgave.personIdent, newOppfolgingsoppgave?.personIdent)
        assertEquals(UserConstants.OTHER_VEILEDER_IDENT, newOppfolgingsoppgave?.sisteVersjon()?.createdBy)
        assertEquals(sisteVersjon.tekst, newOppfolgingsoppgave?.sisteVersjon()?.tekst)
        assertEquals(sisteVersjon.oppfolgingsgrunn, newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn)
        assertEquals(oppfolgingsoppgave.isActive, newOppfolgingsoppgave?.isActive)
        assertEquals(oppfolgingsoppgave.removedBy, newOppfolgingsoppgave?.removedBy)

        assertNotEquals(sisteVersjon.frist, newOppfolgingsoppgave?.sisteVersjon()?.frist)
        assertEquals(newFrist, newOppfolgingsoppgave?.sisteVersjon()?.frist)

        assertNull(newOppfolgingsoppgave?.publishedAt)
        assertTrue(newOppfolgingsoppgave?.updatedAt!! > createdOppfolgingsoppgave.updatedAt)
    }

    @Test
    fun `adds a new version of oppfolgingsoppgave with only edited tekst`() {
        val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

        val sisteVersjon = oppfolgingsoppgave.sisteVersjon()
        val newTekst = sisteVersjon.tekst + " - enda mer informasjon her"
        val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
            existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
            veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
            newOppfolgingsgrunn = sisteVersjon.oppfolgingsgrunn,
            newTekst = newTekst,
            newFrist = sisteVersjon.frist,
        )

        assertEquals(oppfolgingsoppgave.uuid, newOppfolgingsoppgave?.uuid)
        assertEquals(oppfolgingsoppgave.personIdent, newOppfolgingsoppgave?.personIdent)
        assertEquals(UserConstants.OTHER_VEILEDER_IDENT, newOppfolgingsoppgave?.sisteVersjon()?.createdBy)
        assertEquals(sisteVersjon.oppfolgingsgrunn, newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn)
        assertEquals(sisteVersjon.frist, newOppfolgingsoppgave?.sisteVersjon()?.frist)
        assertEquals(oppfolgingsoppgave.isActive, newOppfolgingsoppgave?.isActive)
        assertEquals(oppfolgingsoppgave.publishedAt, newOppfolgingsoppgave?.publishedAt)
        assertEquals(oppfolgingsoppgave.removedBy, newOppfolgingsoppgave?.removedBy)

        assertNotEquals(sisteVersjon.tekst, newOppfolgingsoppgave?.sisteVersjon()?.tekst)
        assertEquals(newTekst, newOppfolgingsoppgave?.sisteVersjon()?.tekst)
        assertNull(newOppfolgingsoppgave?.publishedAt)
    }

    @Test
    fun `adds a new version of oppfolgingsoppgave with edited frist and tekst`() {
        val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

        val sisteVersjon = oppfolgingsoppgave.sisteVersjon()
        val newTekst = sisteVersjon.tekst + " - enda mer informasjon her"
        val newFrist = sisteVersjon.frist!!.plusWeeks(1)
        val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
            existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
            veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
            newOppfolgingsgrunn = sisteVersjon.oppfolgingsgrunn,
            newTekst = newTekst,
            newFrist = newFrist,
        )

        assertEquals(oppfolgingsoppgave.uuid, newOppfolgingsoppgave?.uuid)
        assertEquals(oppfolgingsoppgave.personIdent, newOppfolgingsoppgave?.personIdent)
        assertEquals(UserConstants.OTHER_VEILEDER_IDENT, newOppfolgingsoppgave?.sisteVersjon()?.createdBy)
        assertEquals(sisteVersjon.oppfolgingsgrunn, newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn)
        assertEquals(oppfolgingsoppgave.isActive, newOppfolgingsoppgave?.isActive)
        assertEquals(oppfolgingsoppgave.publishedAt, newOppfolgingsoppgave?.publishedAt)
        assertEquals(oppfolgingsoppgave.removedBy, newOppfolgingsoppgave?.removedBy)

        assertNotEquals(sisteVersjon.frist, newOppfolgingsoppgave?.sisteVersjon()?.frist)
        assertEquals(newFrist, newOppfolgingsoppgave?.sisteVersjon()?.frist)

        assertNotEquals(sisteVersjon.tekst, newOppfolgingsoppgave?.sisteVersjon()?.tekst)
        assertEquals(newTekst, newOppfolgingsoppgave?.sisteVersjon()?.tekst)

        assertNull(newOppfolgingsoppgave?.publishedAt)
    }

    @Test
    fun `deletes current oppfolgingsoppgave and adds new oppfolgingsoppgave when edited oppfolgingsgrunn`() {
        val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

        val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
            existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
            veilederIdent = VEILEDER_IDENT,
            newOppfolgingsgrunn = Oppfolgingsgrunn.VURDER_14A,
            newTekst = oppfolgingsoppgave.sisteVersjon().tekst,
            newFrist = oppfolgingsoppgave.sisteVersjon().frist,
        )

        assertFalse(oppfolgingsoppgaveRepository.getOppfolgingsoppgave(uuid = createdOppfolgingsoppgave.uuid)?.isActive ?: true)

        assertNotEquals(createdOppfolgingsoppgave.uuid, newOppfolgingsoppgave?.uuid)
        assertTrue(newOppfolgingsoppgave?.isActive ?: false)
        assertEquals(Oppfolgingsgrunn.VURDER_14A, newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn)
    }
}
