package no.nav.syfo.infrastructure.kafka.identhendelse

import no.nav.syfo.domain.Oppfolgingsgrunn
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.generateIdenthendelse
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldNotBeEmpty
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

private val aktivIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT
private val inaktivIdent = UserConstants.ARBEIDSTAKER_3_PERSONIDENT

class IdenthendelseServiceSpek : Spek({
    describe(IdenthendelseService::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val oppfolgingsoppgaveRepository = OppfolgingsoppgaveRepository(database = database)
        val identhendelseService = IdenthendelseService(
            oppfolgingsoppgaveRepository = oppfolgingsoppgaveRepository,
        )

        beforeEachTest {
            database.dropData()
        }

        val oppfolgingsoppgaveInaktivIdent = Oppfolgingsoppgave.create(
            personIdent = inaktivIdent,
            veilederIdent = UserConstants.VEILEDER_IDENT,
            tekst = "En god tekst for en oppfolgingsoppgave",
            oppfolgingsgrunn = Oppfolgingsgrunn.TA_KONTAKT_SYKEMELDT,
        )

        it("Flytter oppfølgingsoppgave fra inaktiv ident til ny ident når person får ny ident") {
            oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgaveInaktivIdent)

            val identhendelse = generateIdenthendelse(
                aktivIdent = aktivIdent,
                inaktiveIdenter = listOf(inaktivIdent)
            )
            identhendelseService.handle(identhendelse)

            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).shouldBeEmpty()
            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).shouldNotBeEmpty()
        }

        it("Oppdaterer ingenting når person får ny ident og uten oppfølgingsoppgave på inaktiv ident") {
            val identhendelse = generateIdenthendelse(
                aktivIdent = aktivIdent,
                inaktiveIdenter = listOf(inaktivIdent)
            )
            identhendelseService.handle(identhendelse)

            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).shouldBeEmpty()
            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).shouldBeEmpty()
        }

        it("Oppdaterer ingenting når person får ny ident uten inaktiv identer") {
            oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgaveInaktivIdent)

            val identhendelse = generateIdenthendelse(
                aktivIdent = aktivIdent,
                inaktiveIdenter = emptyList()
            )
            identhendelseService.handle(identhendelse)

            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).shouldNotBeEmpty()
            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).shouldBeEmpty()
        }

        it("Oppdaterer ingenting når person mangler aktiv ident") {
            oppfolgingsoppgaveRepository.create(oppfolgingsoppgave = oppfolgingsoppgaveInaktivIdent)

            val identhendelse = generateIdenthendelse(
                aktivIdent = null,
                inaktiveIdenter = listOf(inaktivIdent)
            )
            identhendelseService.handle(identhendelse)

            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = inaktivIdent).shouldNotBeEmpty()
            oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(personIdent = aktivIdent).shouldBeEmpty()
        }
    }
})
