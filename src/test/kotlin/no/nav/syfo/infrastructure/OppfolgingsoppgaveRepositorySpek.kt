package no.nav.syfo.infrastructure

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

class OppfolgingsoppgaveRepositorySpek : Spek({

    describe(OppfolgingsoppgaveRepository::class.java.simpleName) {

        val database = ExternalMockEnvironment.instance.database
        val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(database = database)

        val oppfolgingsoppgave: Oppfolgingsoppgave =
            Oppfolgingsoppgave.create(
                personIdent = ARBEIDSTAKER_PERSONIDENT,
                veilederIdent = VEILEDER_IDENT,
                tekst = "En god tekst for en oppfolgingsoppgave",
                oppfolgingsgrunn = Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT,
                frist = LocalDate.now().plusDays(1),
            )

        afterEachTest {
            database.dropData()
        }

        describe("create") {
            it("creates an oppfolgingsoppgave") {
                val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
                val retrievedOppfolgingsoppgave =
                    oppfolgingsoppgaveRepository.getPOppfolgingsoppgave(createdOppfolgingsoppgave.uuid)

                createdOppfolgingsoppgave.uuid shouldBeEqualTo retrievedOppfolgingsoppgave?.uuid
                createdOppfolgingsoppgave.personIdent shouldBeEqualTo retrievedOppfolgingsoppgave?.personIdent
                createdOppfolgingsoppgave.createdAt shouldBeEqualTo retrievedOppfolgingsoppgave?.createdAt
                createdOppfolgingsoppgave.isActive shouldBeEqualTo retrievedOppfolgingsoppgave?.isActive
                createdOppfolgingsoppgave.publishedAt shouldBeEqualTo retrievedOppfolgingsoppgave?.publishedAt
                createdOppfolgingsoppgave.removedBy shouldBeEqualTo retrievedOppfolgingsoppgave?.removedBy
            }
        }

        describe("createVersion") {
            it("creates a new version of oppfolgingsoppgave with new frist") {
                val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

                val newFrist = LocalDate.now().plusWeeks(1)

                val editedOppfolgingsoppgave = createdOppfolgingsoppgave.edit(
                    tekst = oppfolgingsoppgave.sisteVersjon().tekst,
                    frist = newFrist,
                    veilederIdent = VEILEDER_IDENT,
                )

                val oppfolgingsoppgaveVersions = oppfolgingsoppgaveRepository.edit(editedOppfolgingsoppgave)?.versjoner!!

                oppfolgingsoppgaveVersions.size shouldBe 2
                oppfolgingsoppgaveVersions[1].frist shouldBeEqualTo oppfolgingsoppgave.sisteVersjon().frist
                oppfolgingsoppgaveVersions[0].frist shouldBeEqualTo newFrist
            }
        }
    }
})
