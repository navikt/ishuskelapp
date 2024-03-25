package no.nav.syfo.infrastructure

import no.nav.syfo.huskelapp.database.HuskelappRepository
import no.nav.syfo.huskelapp.domain.Huskelapp
import no.nav.syfo.huskelapp.domain.Oppfolgingsgrunn
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

class OppfolgingsoppgaveRepositorySpek : Spek({

    describe(HuskelappRepository::class.java.simpleName) {

        val database = ExternalMockEnvironment.instance.database
        val oppfolgingsoppgaveRepository = HuskelappRepository(database = database)

        val oppfolgingsoppgave = Huskelapp.create(
            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            veilederIdent = UserConstants.VEILEDER_IDENT,
            tekst = "En god tekst for en oppfolgingsoppgave",
            oppfolgingsgrunner = listOf(Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT),
            frist = LocalDate.now().plusDays(1),
        )

        afterEachTest {
            database.dropData()
        }

        describe("create") {
            it("creates an oppfolgingsoppgave") {
                val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
                val retrievedOppfolgingsoppgave =
                    oppfolgingsoppgaveRepository.getHuskelapp(createdOppfolgingsoppgave.uuid)

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
                val newFrist = LocalDate.now().plusWeeks(1)
                val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
                val createdOppfolgingsoppgaveId =
                    oppfolgingsoppgaveRepository.getHuskelapp(createdOppfolgingsoppgave.uuid)!!.id
                val newVersion = oppfolgingsoppgave.edit(oppfolgingsoppgave.tekst, newFrist)
                oppfolgingsoppgaveRepository.createVersion(
                    createdOppfolgingsoppgaveId,
                    newOppfolgingsoppgaveVersion = newVersion,
                )

                val oppfolgingsoppgaveVersions =
                    oppfolgingsoppgaveRepository.getHuskelappVersjoner(createdOppfolgingsoppgaveId)

                oppfolgingsoppgaveVersions.size shouldBe 2
                oppfolgingsoppgaveVersions[1].frist shouldBeEqualTo oppfolgingsoppgave.frist
                oppfolgingsoppgaveVersions[0].frist shouldBeEqualTo newFrist
            }
        }
    }
})
