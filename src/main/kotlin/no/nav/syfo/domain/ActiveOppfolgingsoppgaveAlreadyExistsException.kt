package no.nav.syfo.domain

class ActiveOppfolgingsoppgaveAlreadyExistsException(
    personIdent: PersonIdent,
) : RuntimeException("An active oppfolgingsoppgave already exists for this person")
