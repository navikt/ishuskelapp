package no.nav.syfo.huskelapp.domain

import no.nav.syfo.domain.Huskelapp
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.testhelper.UserConstants
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class HuskelappSpek : Spek({

    val huskelapp = Huskelapp.create(
        personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        veilederIdent = UserConstants.VEILEDER_IDENT,
        tekst = "En huskelapp",
        oppfolgingsgrunner = listOf(Oppfolgingsgrunn.VURDER_DIALOGMOTE_SENERE)
    )

    describe("Remove") {
        it("sets huskelapp inactive, removed_by and resets publishedAt") {
            val removedHuskelapp = huskelapp.remove(UserConstants.OTHER_VEILEDER_IDENT)
            removedHuskelapp.isActive.shouldBeFalse()
            removedHuskelapp.removedBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            removedHuskelapp.publishedAt.shouldBeNull()
            removedHuskelapp.updatedAt shouldBeGreaterThan huskelapp.createdAt
        }
    }
})
