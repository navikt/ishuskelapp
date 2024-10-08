package no.nav.syfo.application

import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
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

        val oppfolgingsoppgave = Oppfolgingsoppgave.create(
            personIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            veilederIdent = UserConstants.VEILEDER_IDENT,
            tekst = "En god tekst for en oppfolgingsoppgave",
            oppfolgingsgrunner = listOf(Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT),
            frist = LocalDate.now().plusDays(1),
        )

        afterEachTest {
            database.dropData()
        }

        describe("addVersion") {
            it("adds a new version of oppfolgingsoppgave with only edited frist") {
                val newFrist = oppfolgingsoppgave.frist!!.plusWeeks(1)
                val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
                val publishedOppfolgingsoppgave = createdOppfolgingsoppgave.publish()
                oppfolgingsoppgaveRepository.updatePublished(publishedOppfolgingsoppgave)

                val newOppfolgingsoppgave = oppfolgingsoppgaveService.addVersion(
                    existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                    veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
                    newTekst = oppfolgingsoppgave.tekst,
                    newFrist = newFrist,
                )

                newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
                newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
                newOppfolgingsoppgave?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
                newOppfolgingsoppgave?.tekst shouldBeEqualTo oppfolgingsoppgave.tekst
                newOppfolgingsoppgave?.oppfolgingsgrunner shouldBeEqualTo oppfolgingsoppgave.oppfolgingsgrunner
                newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
                newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

                newOppfolgingsoppgave?.frist shouldNotBeEqualTo oppfolgingsoppgave.frist
                newOppfolgingsoppgave?.frist shouldBeEqualTo newFrist

                newOppfolgingsoppgave?.publishedAt.shouldBeNull()
                newOppfolgingsoppgave?.updatedAt!! shouldBeGreaterThan createdOppfolgingsoppgave.updatedAt
            }
        }

        it("adds a new version of oppfolgingsoppgave with only edited tekst") {
            val newTekst = oppfolgingsoppgave.tekst + " - enda mer informasjon her"
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

            val newOppfolgingsoppgave = oppfolgingsoppgaveService.addVersion(
                existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
                newTekst = newTekst,
                newFrist = oppfolgingsoppgave.frist,
            )

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            newOppfolgingsoppgave?.oppfolgingsgrunner shouldBeEqualTo oppfolgingsoppgave.oppfolgingsgrunner
            newOppfolgingsoppgave?.frist shouldBeEqualTo oppfolgingsoppgave.frist
            newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
            newOppfolgingsoppgave?.publishedAt shouldBeEqualTo oppfolgingsoppgave.publishedAt
            newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

            newOppfolgingsoppgave?.tekst shouldNotBeEqualTo oppfolgingsoppgave.tekst
            newOppfolgingsoppgave?.tekst shouldBeEqualTo newTekst
            newOppfolgingsoppgave?.publishedAt.shouldBeNull()
        }
        it("adds a new version of oppfolgingsoppgave with edited frist and tekst") {
            val newTekst = oppfolgingsoppgave.tekst + " - enda mer informasjon her"
            val newFrist = oppfolgingsoppgave.frist!!.plusWeeks(1)
            val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)

            val newOppfolgingsoppgave = oppfolgingsoppgaveService.addVersion(
                existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                veilederIdent = UserConstants.OTHER_VEILEDER_IDENT,
                newTekst = newTekst,
                newFrist = newFrist,
            )

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.createdBy shouldBeEqualTo UserConstants.OTHER_VEILEDER_IDENT
            newOppfolgingsoppgave?.oppfolgingsgrunner shouldBeEqualTo oppfolgingsoppgave.oppfolgingsgrunner
            newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
            newOppfolgingsoppgave?.publishedAt shouldBeEqualTo oppfolgingsoppgave.publishedAt
            newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

            newOppfolgingsoppgave?.frist shouldNotBeEqualTo oppfolgingsoppgave.frist
            newOppfolgingsoppgave?.frist shouldBeEqualTo newFrist

            newOppfolgingsoppgave?.tekst shouldNotBeEqualTo oppfolgingsoppgave.tekst
            newOppfolgingsoppgave?.tekst shouldBeEqualTo newTekst

            newOppfolgingsoppgave?.publishedAt.shouldBeNull()
        }
    }
})
