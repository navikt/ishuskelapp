package no.nav.syfo.application

import no.nav.syfo.infrastructure.database.repository.HuskelappRepository
import no.nav.syfo.domain.Huskelapp
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

class HuskelappServiceSpek : Spek({

    describe(HuskelappService::class.java.simpleName) {

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val oppfolgingsoppgaveRepository = HuskelappRepository(database = database)
        val oppfolgingsoppgaveService = HuskelappService(oppfolgingsoppgaveRepository)

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

        describe("addVersion") {
            it("adds a new version of oppfolgingsoppgave with only edited frist") {
                val newFrist = oppfolgingsoppgave.frist!!.plusWeeks(1)
                val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
                val publishedOppfolgingsoppgave = createdOppfolgingsoppgave.publish()
                oppfolgingsoppgaveRepository.updatePublished(publishedOppfolgingsoppgave)

                val newOppfolgingsoppgaveVersion = EditedOppfolgingsoppgaveDTO(
                    tekst = oppfolgingsoppgave.tekst,
                    frist = newFrist,
                )
                val newOppfolgingsoppgave = oppfolgingsoppgaveService.addVersion(
                    existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                    newVersion = newOppfolgingsoppgaveVersion
                )

                newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
                newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
                newOppfolgingsoppgave?.createdBy shouldBeEqualTo oppfolgingsoppgave.createdBy
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
            val newOppfolgingsoppgaveVersion = EditedOppfolgingsoppgaveDTO(
                tekst = newTekst,
                frist = oppfolgingsoppgave.frist,
            )
            val newOppfolgingsoppgave = oppfolgingsoppgaveService.addVersion(
                existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                newVersion = newOppfolgingsoppgaveVersion
            )

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.createdBy shouldBeEqualTo oppfolgingsoppgave.createdBy
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
            val newOppfolgingsoppgaveVersion = EditedOppfolgingsoppgaveDTO(
                tekst = newTekst,
                frist = newFrist,
            )
            val newOppfolgingsoppgave = oppfolgingsoppgaveService.addVersion(
                existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                newVersion = newOppfolgingsoppgaveVersion
            )

            newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
            newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
            newOppfolgingsoppgave?.createdBy shouldBeEqualTo oppfolgingsoppgave.createdBy
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
