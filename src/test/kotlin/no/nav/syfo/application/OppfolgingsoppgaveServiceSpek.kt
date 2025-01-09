package no.nav.syfo.application

import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
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

                val newFrist = oppfolgingsoppgave.sisteVersjon().frist!!.plusWeeks(1)
                val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
                    existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                    veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
                    newTekst = oppfolgingsoppgave.sisteVersjon().tekst,
                    newFrist = newFrist,
                )

                newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
                newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
                newOppfolgingsoppgave?.sisteVersjon()?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
                newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldBeEqualTo oppfolgingsoppgave.sisteVersjon().tekst
                newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn shouldBeEqualTo oppfolgingsoppgave.sisteVersjon().oppfolgingsgrunn
                newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
                newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

                newOppfolgingsoppgave?.sisteVersjon()?.frist shouldNotBeEqualTo oppfolgingsoppgave.sisteVersjon().frist
                newOppfolgingsoppgave?.sisteVersjon()?.frist shouldBeEqualTo newFrist

                newOppfolgingsoppgave?.publishedAt.shouldBeNull()
                newOppfolgingsoppgave?.updatedAt!! shouldBeGreaterThan createdOppfolgingsoppgave.updatedAt
            }
        }

        it("adds a new version of oppfolgingsoppgave with only edited tekst") {
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

            val newTekst = oppfolgingsoppgave.sisteVersjon().tekst + " - enda mer informasjon her"
            val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
                existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
                newTekst = newTekst,
                newFrist = oppfolgingsoppgave.sisteVersjon().frist,
            )

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.sisteVersjon()?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn shouldBeEqualTo oppfolgingsoppgave.sisteVersjon().oppfolgingsgrunn
            newOppfolgingsoppgave?.sisteVersjon()?.frist shouldBeEqualTo oppfolgingsoppgave.sisteVersjon().frist
            newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
            newOppfolgingsoppgave?.publishedAt shouldBeEqualTo oppfolgingsoppgave.publishedAt
            newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldNotBeEqualTo oppfolgingsoppgave.sisteVersjon().tekst
            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldBeEqualTo newTekst
            newOppfolgingsoppgave?.publishedAt.shouldBeNull()
        }
        it("adds a new version of oppfolgingsoppgave with edited frist and tekst") {
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

            val newTekst = oppfolgingsoppgave.sisteVersjon().tekst + " - enda mer informasjon her"
            val newFrist = oppfolgingsoppgave.sisteVersjon().frist!!.plusWeeks(1)
            val newOppfolgingsoppgave = oppfolgingsoppgaveService.editOppfolgingsoppgave(
                existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
                newTekst = newTekst,
                newFrist = newFrist,
            )

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.sisteVersjon()?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            newOppfolgingsoppgave?.sisteVersjon()?.oppfolgingsgrunn shouldBeEqualTo oppfolgingsoppgave.sisteVersjon().oppfolgingsgrunn
            newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
            newOppfolgingsoppgave?.publishedAt shouldBeEqualTo oppfolgingsoppgave.publishedAt
            newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

            newOppfolgingsoppgave?.sisteVersjon()?.frist shouldNotBeEqualTo oppfolgingsoppgave.sisteVersjon().frist
            newOppfolgingsoppgave?.sisteVersjon()?.frist shouldBeEqualTo newFrist

            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldNotBeEqualTo oppfolgingsoppgave.sisteVersjon().tekst
            newOppfolgingsoppgave?.sisteVersjon()?.tekst shouldBeEqualTo newTekst

            newOppfolgingsoppgave?.publishedAt.shouldBeNull()
        }
    }
})
