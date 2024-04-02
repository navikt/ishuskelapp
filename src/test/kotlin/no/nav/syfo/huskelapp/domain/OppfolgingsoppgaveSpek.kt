package no.nav.syfo.huskelapp.domain

import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.testhelper.UserConstants
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class OppfolgingsoppgaveSpek : Spek({

    val oppfolgingsoppgave = Oppfolgingsoppgave.create(
        personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        veilederIdent = UserConstants.VEILEDER_IDENT,
        tekst = "En oppfolgingsoppgave",
        oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
    )

    describe("Remove") {
        it("sets oppfolgingsoppgave inactive, removed_by and resets publishedAt") {
            val removedOppfolgingsoppgave = oppfolgingsoppgave.remove(UserConstants.OTHER_VEILEDER_IDENT)
            removedOppfolgingsoppgave.isActive.shouldBeFalse()
            removedOppfolgingsoppgave.removedBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            removedOppfolgingsoppgave.publishedAt.shouldBeNull()
            removedOppfolgingsoppgave.updatedAt shouldBeGreaterThan oppfolgingsoppgave.createdAt
        }
    }
})
