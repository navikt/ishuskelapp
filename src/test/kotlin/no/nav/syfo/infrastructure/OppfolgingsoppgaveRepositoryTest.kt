package no.nav.syfo.infrastructure

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT
import no.nav.syfo.testhelper.dropData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppfolgingsoppgaveRepositoryTest {

    private val database = ExternalMockEnvironment.instance.database
    private val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(database = database)

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

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `creates an oppfolgingsoppgave`() {
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
            val retrievedOppfolgingsoppgave =
                oppfolgingsoppgaveRepository.getPOppfolgingsoppgave(createdOppfolgingsoppgave.uuid)

            assertEquals(createdOppfolgingsoppgave.uuid, retrievedOppfolgingsoppgave?.uuid)
            assertEquals(createdOppfolgingsoppgave.personIdent, retrievedOppfolgingsoppgave?.personIdent)
            assertEquals(createdOppfolgingsoppgave.createdAt, retrievedOppfolgingsoppgave?.createdAt)
            assertEquals(createdOppfolgingsoppgave.isActive, retrievedOppfolgingsoppgave?.isActive)
            assertEquals(createdOppfolgingsoppgave.publishedAt, retrievedOppfolgingsoppgave?.publishedAt)
            assertEquals(createdOppfolgingsoppgave.removedBy, retrievedOppfolgingsoppgave?.removedBy)
        }
    }

    @Nested
    @DisplayName("createVersion")
    inner class CreateVersion {
        @Test
        fun `creates a new version of oppfolgingsoppgave with new frist`() {
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

            val newFrist = LocalDate.now().plusWeeks(1)

            val editedOppfolgingsoppgave = createdOppfolgingsoppgave.edit(
                tekst = oppfolgingsoppgave.sisteVersjon().tekst,
                frist = newFrist,
                veilederIdent = VEILEDER_IDENT,
            )

            val oppfolgingsoppgaveVersions = oppfolgingsoppgaveRepository.edit(editedOppfolgingsoppgave)?.versjoner!!

            assertEquals(2, oppfolgingsoppgaveVersions.size)
            assertEquals(oppfolgingsoppgave.sisteVersjon().frist, oppfolgingsoppgaveVersions[1].frist)
            assertEquals(newFrist, oppfolgingsoppgaveVersions[0].frist)
            assertEquals(oppfolgingsoppgaveVersions[1].tekst, oppfolgingsoppgaveVersions[0].tekst)
        }

        @Test
        fun `creates a new version of oppfolgingsoppgave with new tekst`() {
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
            val newText = "Changed"
            val existingText = createdOppfolgingsoppgave.sisteVersjon().tekst
            val existingFrist = createdOppfolgingsoppgave.sisteVersjon().frist
            val editedOppfolgingsoppgave = createdOppfolgingsoppgave.edit(
                tekst = newText,
                frist = existingFrist,
                veilederIdent = VEILEDER_IDENT,
            )

            val oppfolgingsoppgaveVersions = oppfolgingsoppgaveRepository.edit(editedOppfolgingsoppgave)?.versjoner!!

            assertEquals(2, oppfolgingsoppgaveVersions.size)
            assertEquals(existingFrist, oppfolgingsoppgaveVersions[0].frist)
            assertEquals(newText, oppfolgingsoppgaveVersions[0].tekst)
            assertEquals(existingFrist, oppfolgingsoppgaveVersions[1].frist)
            assertEquals(existingText, oppfolgingsoppgaveVersions[1].tekst)
        }

        @Test
        fun `creates several versions of oppfolgingsoppgave`() {
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
            val existingFrist = createdOppfolgingsoppgave.sisteVersjon().frist

            val updated = createdOppfolgingsoppgave.edit(
                tekst = "changed",
                frist = existingFrist,
                veilederIdent = VEILEDER_IDENT,
            )
            oppfolgingsoppgaveRepository.edit(updated)

            val updatedAgain = updated.edit(
                tekst = "changedAgain",
                frist = existingFrist,
                veilederIdent = VEILEDER_IDENT,
            )

            val oppfolgingsoppgaveVersions = oppfolgingsoppgaveRepository.edit(updatedAgain)?.versjoner!!

            assertEquals(3, oppfolgingsoppgaveVersions.size)
            assertEquals("changedAgain", oppfolgingsoppgaveVersions[0].tekst)
        }
    }
}
