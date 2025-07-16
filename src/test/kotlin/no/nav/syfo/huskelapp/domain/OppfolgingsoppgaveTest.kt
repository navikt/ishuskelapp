package no.nav.syfo.huskelapp.domain

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.testhelper.UserConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class OppfolgingsoppgaveTest {

    private val oppfolgingsoppgave = Oppfolgingsoppgave.create(
        personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        veilederIdent = UserConstants.VEILEDER_IDENT,
        tekst = "En oppfolgingsoppgave",
        oppfolgingsgrunn = Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE
    )

    @Test
    fun `sets oppfolgingsoppgave inactive, removed_by and resets publishedAt`() {
        val removedOppfolgingsoppgave = oppfolgingsoppgave.remove(UserConstants.OTHER_VEILEDER_IDENT)
        assertFalse(removedOppfolgingsoppgave.isActive)
        assertEquals(UserConstants.OTHER_VEILEDER_IDENT, removedOppfolgingsoppgave.removedBy)
        assertNull(removedOppfolgingsoppgave.publishedAt)
        assertTrue(removedOppfolgingsoppgave.updatedAt > oppfolgingsoppgave.createdAt)
    }
}
