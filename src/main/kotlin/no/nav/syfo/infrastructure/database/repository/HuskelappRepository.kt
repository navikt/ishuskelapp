package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Huskelapp
import no.nav.syfo.infrastructure.database.*
import no.nav.syfo.util.nowUTC
import java.sql.*
import java.sql.Date
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

    fun create(huskelapp: Huskelapp): Huskelapp {
        database.connection.use { connection ->
            val createdHuskelapp = connection.createHuskelapp(huskelapp)
            val createdVersion = connection.createHuskelappVersjon(
                huskelappId = createdHuskelapp.id,
                newOppfolgingsoppgaveVersion = huskelapp,
            )
            connection.commit()
            return createdHuskelapp.toHuskelapp(createdVersion)
        }
    }

    fun createVersion(
        huskelappId: Int,
        newOppfolgingsoppgaveVersion: Huskelapp,
    ): PHuskelappVersjon =
        database.connection.use { connection ->
            val huskelappVersjon = connection.createHuskelappVersjon(
                huskelappId = huskelappId,
                newOppfolgingsoppgaveVersion = newOppfolgingsoppgaveVersion,
            )
            connection.updatePublished(huskelapp = newOppfolgingsoppgaveVersion)
            connection.commit()
            return huskelappVersjon
        }

    private fun Connection.createHuskelappVersjon(
        huskelappId: Int,
        newOppfolgingsoppgaveVersion: Huskelapp,
    ): PHuskelappVersjon {
        val idList = this.prepareStatement(CREATE_HUSKELAPP_VERSJON_QUERY).use {
            it.setString(1, UUID.randomUUID().toString())
            it.setInt(2, huskelappId)
            it.setObject(3, nowUTC())
            it.setString(4, newOppfolgingsoppgaveVersion.createdBy)
            newOppfolgingsoppgaveVersion.tekst
                ?.let { tekst -> it.setString(5, tekst) }
                ?: it.setNull(5, Types.LONGVARCHAR)
            if (newOppfolgingsoppgaveVersion.oppfolgingsgrunner.isEmpty()) {
                it.setNull(6, Types.LONGVARCHAR)
            } else {
                it.setString(6, newOppfolgingsoppgaveVersion.oppfolgingsgrunner.joinToString(","))
            }
            it.setDate(7, newOppfolgingsoppgaveVersion.frist?.let { frist -> Date.valueOf(frist) })
            it.executeQuery().toList { toPHuskelappVersjon() }
        }

        if (idList.size != 1) {
            throw NoElementInsertedException("Creating HUSKELAPP_VERSJON failed, no rows affected.")
        }

        return idList.first()
    }

    fun getUnpublished(): List<PHuskelapp> = database.getUnpublishedHuskelapper()
    fun updatePublished(huskelapp: Huskelapp) = database.updatePublished(huskelapp = huskelapp)
    fun updateRemovedHuskelapp(huskelapp: Huskelapp) = database.updateRemovedHuskelapp(huskelapp = huskelapp)

    companion object {
        private const val CREATE_HUSKELAPP_VERSJON_QUERY =
            """
            INSERT INTO HUSKELAPP_VERSJON (
                id,
                uuid,
                huskelapp_id,
                created_at,
                created_by,
                tekst,
                oppfolgingsgrunner,
                frist
            ) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """
    }
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
    RETURNING *
    """

private fun Connection.createHuskelapp(
    huskelapp: Huskelapp,
): PHuskelapp {
    val now = nowUTC()
    val idList = this.prepareStatement(queryCreateHuskelapp).use {
        it.setString(1, huskelapp.uuid.toString())
        it.setString(2, huskelapp.personIdent.value)
        it.setObject(3, now)
        it.setObject(4, now)
        it.setObject(5, huskelapp.isActive)
        it.executeQuery().toList { toPHuskelapp() }
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

private fun DatabaseInterface.updatePublished(huskelapp: Huskelapp) = this.connection.use { connection ->
    connection.updatePublished(huskelapp)
    connection.commit()
}

private fun Connection.updatePublished(huskelapp: Huskelapp) {
    this.prepareStatement(querySetPublished).use {
        it.setObject(1, huskelapp.publishedAt)
        it.setObject(2, huskelapp.updatedAt)
        it.setString(3, huskelapp.uuid.toString())
        val updated = it.executeUpdate()
        if (updated != 1) {
            throw SQLException("Expected a single row to be updated, got update count $updated")
        }
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
    oppfolgingsgrunner = getString("oppfolgingsgrunner").split(",").map(String::trim).filter(String::isNotEmpty),
    frist = getDate("frist")?.toLocalDate(),
)
