package no.nav.syfo.testhelper

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.infrastructure.database.DatabaseInterface
import org.flywaydb.core.Flyway
import java.sql.Connection

class TestDatabase : DatabaseInterface {
    private val pg: EmbeddedPostgres = try {
        EmbeddedPostgres.start()
    } catch (e: Exception) {
        EmbeddedPostgres.builder().setLocaleConfig("locale", "en_US").start()
    }

    private var shouldSimulateError = false
    private val workingConnection: Connection
        get() = pg.postgresDatabase.connection.apply { autoCommit = false }

    override val connection: Connection
        get() = if (shouldSimulateError) {
            throw Exception("Simulated database connection failure")
        } else {
            workingConnection
        }

    init {

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).validateMigrationNaming(true).load().migrate()
        }
    }

    fun stop() {
        pg.close()
    }

    fun simulateDatabaseError() {
        shouldSimulateError = true
    }

    fun restoreDatabase() {
        shouldSimulateError = false
    }

    fun resetDatabase() {
        restoreDatabase()
        dropData()
    }
}

fun DatabaseInterface.dropData() {
    val queryList = listOf(
        """
            DELETE FROM HUSKELAPP_VERSJON
        """.trimIndent(),
        """
            DELETE FROM HUSKELAPP
        """.trimIndent(),
    )
    this.connection.use { connection ->
        queryList.forEach { query ->
            connection.prepareStatement(query).execute()
        }
        connection.commit()
    }
}

class TestDatabaseNotResponding : DatabaseInterface {

    override val connection: Connection
        get() = throw Exception("Not working")

    fun stop() {
    }
}
