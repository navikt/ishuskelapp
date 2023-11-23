package no.nav.syfo.huskelapp.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.NoElementInsertedException
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.domain.Huskelapp
import no.nav.syfo.util.nowUTC
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class HuskelappRepository(
    private val database: DatabaseInterface,
) {
    fun getHuskelapper(personIdent: PersonIdent): List<PHuskelapp> {
        return database.getHuskelapper(personIdent)
    }

    fun getHuskelapp(uuid: UUID): PHuskelapp? {
        return database.getHuskelapp(uuid)
    }

    fun getHuskelappVersjoner(huskelappId: Int): List<PHuskelappVersjon> {
        return database.getHuskelappVersjoner(huskelappId)
    }

    fun createVersjon(huskelappId: Int, veilederIdent: String, tekst: String, frist: LocalDate?) {
        database.connection.use { connection ->
            connection.createHuskelappVersjon(
                huskelappId = huskelappId,
                createdBy = veilederIdent,
                tekst = tekst,
                frist = frist,
            )
            connection.commit()
        }
    }

    fun create(huskelapp: Huskelapp) {
        database.connection.use { connection ->
            val huskelappId = connection.createHuskelapp(huskelapp)
            connection.createHuskelappVersjon(
                huskelappId = huskelappId,
                createdBy = huskelapp.createdBy,
                tekst = huskelapp.tekst,
                frist = huskelapp.frist,
            )
            connection.commit()
        }
    }

    fun getUnpublished(): List<PHuskelapp> = database.getUnpublishedHuskelapper()
    fun setPublished(huskelapp: Huskelapp) = database.setPublished(huskelapp = huskelapp)
    fun updateRemovedHuskelapp(huskelapp: Huskelapp) = database.updateRemovedHuskelapp(huskelapp = huskelapp)
}

private const val queryCreateHuskelappVersjon =
    """
    INSERT INTO HUSKELAPP_VERSJON (
        id,
        uuid,
        huskelapp_id,
        created_at,
        created_by,
        tekst,
        frist
    ) values (DEFAULT, ?, ?, ?, ?, ?, ?)
    RETURNING id
    """

private fun Connection.createHuskelappVersjon(
    huskelappId: Int,
    createdBy: String,
    tekst: String,
    frist: LocalDate?,
): Int {
    val idList = this.prepareStatement(queryCreateHuskelappVersjon).use {
        it.setString(1, UUID.randomUUID().toString())
        it.setInt(2, huskelappId)
        it.setObject(3, nowUTC())
        it.setString(4, createdBy)
        it.setString(5, tekst)
        it.setDate(6, frist?.let { frist -> Date.valueOf(frist) })
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw NoElementInsertedException("Creating HUSKELAPP_VERSJON failed, no rows affected.")
    }

    return idList.first()
}

private const val queryCreateHuskelapp =
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

private const val queryGetHuskelappByPersonIdent = """
    SELECT *
    FROM HUSKELAPP
    WHERE personident = ?
    ORDER BY created_at DESC
"""

private fun DatabaseInterface.getHuskelapper(personIdent: PersonIdent): List<PHuskelapp> {
    return connection.use { connection ->
        connection.prepareStatement(queryGetHuskelappByPersonIdent).use {
            it.setString(1, personIdent.value)
            it.executeQuery().toList { toPHuskelapp() }
        }
    }
}

private const val queryGetHuskelappByUuid = """
    SELECT *
    FROM HUSKELAPP
    WHERE uuid = ?
"""

private fun DatabaseInterface.getHuskelapp(uuid: UUID): PHuskelapp? {
    return connection.use { connection ->
        connection.prepareStatement(queryGetHuskelappByUuid).use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList { toPHuskelapp() }.firstOrNull()
        }
    }
}

private const val queryGetHuskelappVersjon = """
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

private const val queryGetUnpublishedHuskelapper = """
    SELECT * 
    FROM huskelapp 
    WHERE published_at IS NULL
    ORDER BY created_at ASC
"""

private fun DatabaseInterface.getUnpublishedHuskelapper(): List<PHuskelapp> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetUnpublishedHuskelapper).use {
            it.executeQuery().toList { toPHuskelapp() }
        }
    }
}

private const val querySetPublished = """
    UPDATE huskelapp
    SET published_at = ?, updated_at = ?
    WHERE uuid = ?
"""

private fun DatabaseInterface.setPublished(huskelapp: Huskelapp) {
    val now = nowUTC()
    this.connection.use { connection ->
        connection.prepareStatement(querySetPublished).use {
            it.setObject(1, now)
            it.setObject(2, now)
            it.setString(3, huskelapp.uuid.toString())
            val updated = it.executeUpdate()
            if (updated != 1) {
                throw SQLException("Expected a single row to be updated, got update count $updated")
            }
        }
        connection.commit()
    }
}

private const val queryUpdateRemoved = """
    UPDATE huskelapp
    SET is_active = ?, removed_by = ?, published_at = ?, updated_at = ?
    WHERE uuid = ?
"""

private fun DatabaseInterface.updateRemovedHuskelapp(huskelapp: Huskelapp) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdateRemoved).use {
            it.setBoolean(1, huskelapp.isActive)
            it.setString(2, huskelapp.removedBy)
            it.setObject(3, huskelapp.publishedAt)
            it.setObject(4, huskelapp.updatedAt)
            it.setString(5, huskelapp.uuid.toString())
            val updated = it.executeUpdate()
            if (updated != 1) {
                throw SQLException("Expected a single row to be updated, got update count $updated")
            }
        }
        connection.commit()
    }
}

private fun ResultSet.toPHuskelapp() = PHuskelapp(
    id = getInt("id"),
    uuid = UUID.fromString(getString("uuid")),
    personIdent = PersonIdent(getString("personident")),
    createdAt = getObject("created_at", OffsetDateTime::class.java),
    updatedAt = getObject("updated_at", OffsetDateTime::class.java),
    isActive = getBoolean("is_active"),
    publishedAt = getObject("published_at", OffsetDateTime::class.java),
    removedBy = getString("removed_by"),
)

private fun ResultSet.toPHuskelappVersjon() = PHuskelappVersjon(
    id = getInt("id"),
    uuid = UUID.fromString(getString("uuid")),
    huskelappId = getInt("huskelapp_id"),
    createdAt = getObject("created_at", OffsetDateTime::class.java),
    createdBy = getString("created_by"),
    tekst = getString("tekst"),
    frist = getDate("frist")?.toLocalDate(),
)
