package no.nav.syfo.application

import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.huskelapp.api.EditedOppfolgingsoppgaveDTO
import no.nav.syfo.huskelapp.database.HuskelappRepository
import no.nav.syfo.huskelapp.domain.Huskelapp
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.shouldBeEqualTo
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
            oppfolgingsgrunner = listOf("TA_KONTAKT_SYKEMELDT"),
            frist = LocalDate.now().plusDays(1),
        )

        afterEachTest {
            database.dropData()
        }

        describe("addVersion") {
            it("adds a new version of oppfolgingsoppgave with only edited frist") {
                val newFrist = oppfolgingsoppgave.frist!!.plusWeeks(1)
                val createdOppfolgingsoppgave = oppfolgingsoppgaveRepository.create(oppfolgingsoppgave)
                val newOppfolgingsoppgaveVersion = EditedOppfolgingsoppgaveDTO(
                    tekst = oppfolgingsoppgave.tekst,
                    frist = newFrist,
                )
                val newOppfolgingsoppgave = oppfolgingsoppgaveService.addVersion(
                    existingOppfolgingsoppgaveUuid = createdOppfolgingsoppgave.uuid,
                    newVersion = newOppfolgingsoppgaveVersion
                )

                newOppfolgingsoppgave?.uuid shouldBeEqualTo createdOppfolgingsoppgave.uuid
                newOppfolgingsoppgave?.personIdent shouldBeEqualTo createdOppfolgingsoppgave.personIdent
                newOppfolgingsoppgave?.createdBy shouldBeEqualTo createdOppfolgingsoppgave.createdBy
                newOppfolgingsoppgave?.tekst shouldBeEqualTo createdOppfolgingsoppgave.tekst
                newOppfolgingsoppgave?.oppfolgingsgrunner shouldBeEqualTo createdOppfolgingsoppgave.oppfolgingsgrunner
                newOppfolgingsoppgave?.isActive shouldBeEqualTo createdOppfolgingsoppgave.isActive
                newOppfolgingsoppgave?.createdAt shouldBeEqualTo createdOppfolgingsoppgave.createdAt
                newOppfolgingsoppgave?.publishedAt shouldBeEqualTo createdOppfolgingsoppgave.publishedAt
                newOppfolgingsoppgave?.removedBy shouldBeEqualTo createdOppfolgingsoppgave.removedBy

                newOppfolgingsoppgave?.uuid shouldBeEqualTo oppfolgingsoppgave.uuid
                newOppfolgingsoppgave?.personIdent shouldBeEqualTo oppfolgingsoppgave.personIdent
                newOppfolgingsoppgave?.createdBy shouldBeEqualTo oppfolgingsoppgave.createdBy
                newOppfolgingsoppgave?.tekst shouldBeEqualTo oppfolgingsoppgave.tekst
                newOppfolgingsoppgave?.oppfolgingsgrunner shouldBeEqualTo oppfolgingsoppgave.oppfolgingsgrunner
                newOppfolgingsoppgave?.isActive shouldBeEqualTo oppfolgingsoppgave.isActive
                newOppfolgingsoppgave?.publishedAt shouldBeEqualTo oppfolgingsoppgave.publishedAt
                newOppfolgingsoppgave?.removedBy shouldBeEqualTo oppfolgingsoppgave.removedBy

                newOppfolgingsoppgave?.frist shouldNotBeEqualTo createdOppfolgingsoppgave.frist
                createdOppfolgingsoppgave.frist shouldBeEqualTo oppfolgingsoppgave.frist
                newOppfolgingsoppgave?.frist shouldBeEqualTo newFrist
            }
        }
    }
})
