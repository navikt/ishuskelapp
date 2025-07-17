package no.nav.syfo.infrastructure.kafka.identhendelse

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.generateIdenthendelse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

private val aktivIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT
private val inaktivIdent = UserConstants.ARBEIDSTAKER_3_PERSONIDENT

class IdenthendelseServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(database = database)
    private val identhendelseService = IdenthendelseService(
        oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
    )

    private val oppfolgingsoppgaveInaktivIdent = Oppfolgingsoppgave.create(
        personIdent = inaktivIdent,
        veilederIdent = UserConstants.VEILEDER_IDENT,
        tekst = "En god tekst for en oppfolgingsoppgave",
        oppfolgingsgrunn = Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT,
    )

    @AfterEach
    fun tearDown() {
        database.dropData()
    }

    @Test
    fun `Flytter oppfølgingsoppgave fra inaktiv ident til ny ident når person får ny ident`() {
        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgaveInaktivIdent)

        val identhendelse = generateIdenthendelse(
            aktivIdent = aktivIdent,
            inaktiveIdenter = listOf(inaktivIdent)
        )
        identhendelseService.handle(identhendelse)

        assertTrue(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).isEmpty())
        assertFalse(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).isEmpty())
    }

    @Test
    fun `Oppdaterer ingenting når person får ny ident og uten oppfølgingsoppgave på inaktiv ident`() {
        val identhendelse = generateIdenthendelse(
            aktivIdent = aktivIdent,
            inaktiveIdenter = listOf(inaktivIdent)
        )
        identhendelseService.handle(identhendelse)

        assertTrue(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).isEmpty())
        assertTrue(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).isEmpty())
    }

    @Test
    fun `Oppdaterer ingenting når person får ny ident uten inaktiv identer`() {
        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgaveInaktivIdent)

        val identhendelse = generateIdenthendelse(
            aktivIdent = aktivIdent,
            inaktiveIdenter = emptyList()
        )
        identhendelseService.handle(identhendelse)

        assertFalse(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).isEmpty())
        assertTrue(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).isEmpty())
    }

    @Test
    fun `Oppdaterer ingenting når person mangler aktiv ident`() {
        oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgaveInaktivIdent)

        val identhendelse = generateIdenthendelse(
            aktivIdent = null,
            inaktiveIdenter = listOf(inaktivIdent)
        )
        identhendelseService.handle(identhendelse)

        assertFalse(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).isEmpty())
        assertTrue(oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).isEmpty())
    }
}
