package no.nav.syfo.huskelapp.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.NoElementInsertedException
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.domain.Huskelapp
import no.nav.syfo.util.nowUTC
import java.sql.Connection
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*

class HuskelappRepository(
    private val database: DatabaseInterface,
) {
    fun getHuskelapper(personIdent: PersonIdent): List<PHuskelapp> {
        return database.getHuskelapper(personIdent)
    }

    fun getHuskelappVersjoner(huskelappId: Int): List<PHuskelappVersjon> {
        return database.getHuskelappVersjoner(huskelappId)
    }

    fun createVersjon(huskelappId: Int, veilederIdent: String, tekst: String) {
        database.connection.use { connection ->
            connection.createHuskelappVersjon(
                huskelappId = huskelappId,
                veilederIdent = veilederIdent,
                tekst = tekst,
            )
            connection.commit()
        }
    }

    fun create(huskelapp: Huskelapp) {
        database.connection.use { connection ->
            val huskelappId = connection.createHuskelapp(huskelapp)
            connection.createHuskelappVersjon(
                huskelappId = huskelappId,
                veilederIdent = huskelapp.veilederIdent,
                tekst = huskelapp.tekst,
            )
            connection.commit()
        }
    }
}

const val queryCreateHuskelappVersjon =
    """
    INSERT INTO HUSKELAPP_VERSJON (
        id,
        uuid,
        huskelapp_id,
        created_at,
        created_by,
        tekst
    ) values (DEFAULT, ?, ?, ?, ?, ?)
    RETURNING id
    """

private fun Connection.createHuskelappVersjon(
    huskelappId: Int,
    veilederIdent: String,
    tekst: String,
): Int {
    val idList = this.prepareStatement(queryCreateHuskelappVersjon).use {
        it.setString(1, UUID.randomUUID().toString())
        it.setInt(2, huskelappId)
        it.setObject(3, nowUTC())
        it.setString(4, veilederIdent)
        it.setString(5, tekst)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw NoElementInsertedException("Creating HUSKELAPP_VERSJON failed, no rows affected.")
    }

    return idList.first()
}

const val queryCreateHuskelapp =
    """
    INSERT INTO HUSKELAPP (
        id,
        uuid,
        personident,
        created_at,
        updated_at,
        is_active
    ) values (DEFAULT, ?, ?, ?, ?, ?)
    RETURNING id
    """

private fun Connection.createHuskelapp(
    huskelapp: Huskelapp,
): Int {
    val now = nowUTC()
    val idList = this.prepareStatement(queryCreateHuskelapp).use {
        it.setString(1, huskelapp.uuid.toString())
        it.setString(2, huskelapp.personIdent.value)
        it.setObject(3, now)
        it.setObject(4, now)
        it.setObject(5, huskelapp.isActive)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw NoElementInsertedException("Creating HUSKELAPP failed, no rows affected.")
    }

    return idList.first()
}

const val queryGetHuskelapp = """
    SELECT *
    FROM HUSKELAPP
    WHERE personident = ?
    ORDER BY created_at DESC
"""

private fun DatabaseInterface.getHuskelapper(personIdent: PersonIdent): List<PHuskelapp> {
    return connection.use { connection ->
        connection.prepareStatement(queryGetHuskelapp).use {
            it.setString(1, personIdent.value)
            it.executeQuery().toList { toPHuskelapp() }
        }
    }
}

const val queryGetHuskelappVersjon = """
    SELECT *
    FROM HUSKELAPP_VERSJON
    WHERE huskelapp_id = ?
    ORDER BY created_at DESC
"""

private fun DatabaseInterface.getHuskelappVersjoner(huskelappId: Int): List<PHuskelappVersjon> {
    return connection.use { connection ->
        connection.prepareStatement(queryGetHuskelappVersjon).use {
            it.setInt(1, huskelappId)
            it.executeQuery().toList { toPHuskelappVersjon() }
        }
    }
}

private fun ResultSet.toPHuskelapp() = PHuskelapp(
    id = getInt("id"),
    uuid = UUID.fromString(getString("uuid")),
    personIdent = PersonIdent(getString("personident")),
    createdAt = getObject("created_at", OffsetDateTime::class.java),
    updatedAt = getObject("updated_at", OffsetDateTime::class.java),
    isActive = getBoolean("is_active"),
)

private fun ResultSet.toPHuskelappVersjon() = PHuskelappVersjon(
    id = getInt("id"),
    uuid = UUID.fromString(getString("uuid")),
    huskelappId = getInt("huskelapp_id"),
    createdAt = getObject("created_at", OffsetDateTime::class.java),
    createdBy = getString("created_by"),
    tekst = getString("tekst"),
)
