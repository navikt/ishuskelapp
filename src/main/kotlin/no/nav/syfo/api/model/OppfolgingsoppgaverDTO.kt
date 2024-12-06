package no.nav.syfo.api.model

data class OppfolgingsoppgaverRequestDTO(
    val personidenter: List<String>
)

data class OppfolgingsoppgaverResponseDTO(
    val oppfolgingsoppgaver: Map<String, OppfolgingsoppgaveResponseDTO>
)

data class OppfolgingsoppgaverNewResponseDTO(
    val oppfolgingsoppgaver: Map<String, OppfolgingsoppgaveNewResponseDTO>
)
