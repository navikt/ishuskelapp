![Build status](https://github.com/navikt/ishuskelapp/workflows/main/badge.svg?branch=master)

# ishuskelapp

Applikasjon for å skrive oppfølgingsoppgave

## Technologies used

* Docker
* Gradle
* Kotlin
* Ktor
* Postgres

##### Test Libraries:

* Kluent
* Mockk
* Spek

#### Requirements

* JDK 21

### Build

Run `./gradlew clean shadowJar`

### Lint (Ktlint)

##### Command line

Run checking: `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`

##### Git Hooks

Apply checking: `./gradlew addKtlintCheckGitPreCommitHook`

Apply formatting: `./gradlew addKtlintFormatGitPreCommitHook`
