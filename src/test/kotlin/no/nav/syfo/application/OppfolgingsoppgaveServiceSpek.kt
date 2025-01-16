package no.nav.syfo.application

import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

class OppfolgingsoppgaveServiceSpek : Spek({

    describe(OppfolgingsoppgaveService::class.java.simpleName) {

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(database = database)
        val oppfolgingsoppgaveService = OppfolgingsoppgaveService(oppfolgingsoppgaveRepository)

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

        describe("editOppfolgingsoppgave") {
            it("adds a new version of oppfolgingsoppgave with only edited frist") {
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

                newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
                newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
                newOppfolgingsoppgave?.sisteVersjon()?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
                newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldBeEqualTo sisteVersjon.tekst
                newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn shouldBeEqualTo sisteVersjon.oppfolgingsgrunn
                newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
                newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

                newOppfolgingsoppgave?.sisteVersjon()?.frist shouldNotBeEqualTo sisteVersjon.frist
                newOppfolgingsoppgave?.sisteVersjon()?.frist shouldBeEqualTo newFrist

                newOppfolgingsoppgave?.publishedAt.shouldBeNull()
                newOppfolgingsoppgave?.updatedAt!! shouldBeGreaterThan createdOppfolgingsoppgave.updatedAt
            }
        }

        it("adds a new version of oppfolgingsoppgave with only edited tekst") {
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

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.sisteVersjon()?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn shouldBeEqualTo sisteVersjon.oppfolgingsgrunn
            newOppfolgingsoppgave?.sisteVersjon()?.frist shouldBeEqualTo sisteVersjon.frist
            newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
            newOppfolgingsoppgave?.publishedAt shouldBeEqualTo oppfolgingsoppgave.publishedAt
            newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldNotBeEqualTo sisteVersjon.tekst
            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldBeEqualTo newTekst
            newOppfolgingsoppgave?.publishedAt.shouldBeNull()
        }
        it("adds a new version of oppfolgingsoppgave with edited frist and tekst") {
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

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.sisteVersjon()?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn shouldBeEqualTo sisteVersjon.oppfolgingsgrunn
            newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
            newOppfolgingsoppgave?.publishedAt shouldBeEqualTo oppfolgingsoppgave.publishedAt
            newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

            newOppfolgingsoppgave?.sisteVersjon()?.frist shouldNotBeEqualTo sisteVersjon.frist
            newOppfolgingsoppgave?.sisteVersjon()?.frist shouldBeEqualTo newFrist

            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldNotBeEqualTo sisteVersjon.tekst
            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldBeEqualTo newTekst

            newOppfolgingsoppgave?.publishedAt.shouldBeNull()
        }
        it("deletes current oppfolgingsoppgave and adds new oppfolgingsoppgave when edited oppfolgingsgrunn") {
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

            val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
                existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                veilederIdent = VEILEDER_IDENT,
                newOppfolgingsgrunn = Oppfolgingsgrunn.VURDER_14A,
                newTekst = oppfolgingsoppgave.sisteVersjon().tekst,
                newFrist = oppfolgingsoppgave.sisteVersjon().frist,
            )

            oppfolgingsoppgaveRepository.getOppfolgingsoppgave(uuid = createdOppfolgingsoppgave.uuid)?.isActive?.shouldBeFalse()

            newOppfolgingsoppgave?.uuid shouldNotBeEqualTo createdOppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.isActive?.shouldBeTrue()
            newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn shouldBeEqualTo Oppfolgingsgrunn.VURDER_14A
        }
    }
})
