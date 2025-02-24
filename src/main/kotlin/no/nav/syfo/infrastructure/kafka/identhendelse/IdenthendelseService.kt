package no.nav.syfo.infrastructure.kafka.identhendelse

import no.nav.syfo.infrastructure.database.repository.OppfolgingsoppgaveRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IdenthendelseService(private val oppfolgingsoppgaveRepository: OppfolgingsoppgaveRepository) {

    fun handle(identhendelse: KafkaIdenthendelseDTO) {
        val (aktivIdent, inaktiveIdenter) = identhendelse.getFolkeregisterIdenter()
        if (aktivIdent != null) {
            val oppfolgingsoppgaverMedInaktivIdent = inaktiveIdenter.flatMap { oppfolgingsoppgaveRepository.getOppfolgingsoppgaver(it) }

            if (oppfolgingsoppgaverMedInaktivIdent.isNotEmpty()) {
                oppfolgingsoppgaveRepository.updatePersonident(
                    nyPersonident = aktivIdent,
                    oppfolgingsoppgaver = oppfolgingsoppgaverMedInaktivIdent,
                )
                log.info("Identhendelse: Updated ${oppfolgingsoppgaverMedInaktivIdent.size} oppfølgingsoppgaver based on Identhendelse from PDL")
            }
        } else {
            log.warn("Identhendelse ignored - Mangler aktiv ident i PDL")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
