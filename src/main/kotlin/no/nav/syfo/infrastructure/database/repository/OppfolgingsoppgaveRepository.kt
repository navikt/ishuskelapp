package no.nav.syfo.infrastructure.database.repository

import IOppfolgingsoppgaveRepository
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.NoElementInsertedException
import no.nav.syfo.infrastructure.database.repository.extension.setDateOrNull
import no.nav.syfo.infrastructure.database.repository.extension.setStringOrNull
import no.nav.syfo.infrastructure.database.toList
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.*

class OppfolgingsoppgaveRepository(
    private val database: DatabaseInterface,
) : IOppfolgingsoppgaveRepository {
    override fun getPOppfolgingsoppgaver(personIdent: PersonIdent): List<POppfolgingsoppgave> {
        return database.getOppfolgingsoppgaver(personIdent)
    }

    override fun getOppfolgingsoppgaver(personIdent: PersonIdent): List<Oppfolgingsoppgave> =
        getPOppfolgingsoppgaver(personIdent).map { it.toOppfolgingsoppgave() }

    private fun POppfolgingsoppgave.toOppfolgingsoppgave(): Oppfolgingsoppgave {
        val versjoner = getOppfolgingsoppgaveVersjoner(this.id)
        return this.toOppfolgingsoppgave(versjoner)
    }

    override fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Oppfolgingsoppgave> =
        database.getActiveOppfolgingsoppgaver(personidenter)

    override fun getPOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave? {
        return database.getPOppfolgingsoppgave(uuid)
    }

    override fun getOppfolgingsoppgave(uuid: UUID): Oppfolgingsoppgave? {
        return database.getPOppfolgingsoppgave(uuid)?.toOppfolgingsoppgave()
    }

    override fun getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon> {
        return database.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId)
    }

    override fun create(oppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave {
        database.connection.use { connection ->
            val createdOppfolgingsoppgave = connection.createOppfolgingsoppgave(oppfolgingsoppgave)
            val createdVersion = connection.createOppfolgingsoppgaveVersjon(
                oppfolgingsoppgaveId = createdOppfolgingsoppgave.id,
                newOppfolgingsoppgave = oppfolgingsoppgave,
            )
            connection.commit()
            return createdOppfolgingsoppgave.toOppfolgingsoppgave(listOf(createdVersion))
        }
    }

    override fun edit(existingOppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave? {
        return getPOppfolgingsoppgave(existingOppfolgingsoppgave.uuid)
            ?.let { pExistingOppfolgingsoppgave ->
                updateOppfolgingsoppgaveMedVersjon(pExistingOppfolgingsoppgave.id, existingOppfolgingsoppgave)
            }?.let {
                getOppfolgingsoppgave(it)
            }
    }

    fun updateOppfolgingsoppgaveMedVersjon(
        oppfolgingsoppgaveId: Int,
        oppfolgingsoppgave: Oppfolgingsoppgave
    ): UUID {
        database.connection.use { connection ->
            connection.moveOppfolgingsoppgaveVersjon(oppfolgingsoppgaveId)
            connection.removeOppfolgingsoppgaveVersjonLatest(oppfolgingsoppgaveId)
            connection.createOppfolgingsoppgaveVersjon(oppfolgingsoppgaveId, oppfolgingsoppgave)
            connection.updateOppfolgingsoppgave(oppfolgingsoppgave)
            connection.updatePublished(oppfolgingsoppgave)
            connection.commit()
            COUNT_HUSKELAPP_VERSJON_CREATED.increment()
            return oppfolgingsoppgave.uuid
        }
    }

    override fun remove(oppfolgingsoppgave: Oppfolgingsoppgave) =
        database.updateRemovedOppfolgingsoppgave(oppfolgingsoppgave)

    private fun Connection.createOppfolgingsoppgaveVersjon(
        oppfolgingsoppgaveId: Int,
        newOppfolgingsoppgave: Oppfolgingsoppgave,
    ): POppfolgingsoppgaveVersjon {
        val newVersjon = newOppfolgingsoppgave.versjoner.first()
        val idList = this.prepareStatement(CREATE_OPPFOLGINGSOPPGAVE_VERSJON_LATEST_QUERY).use {
            it.setString(1, UUID.randomUUID().toString())
            it.setInt(2, oppfolgingsoppgaveId)
            it.setObject(3, newVersjon.createdAt)
            it.setString(4, newVersjon.createdBy)
            it.setStringOrNull(5, newVersjon.tekst)
            it.setStringOrNull(6, newVersjon.oppfolgingsgrunn.toString())
            it.setDateOrNull(7, newVersjon.frist?.let { date -> Date.valueOf(date) })
            it.executeQuery().toList { toPOppfolgingsoppgaveVersjon() }
        }

        if (idList.size != 1) {
            throw NoElementInsertedException("Creating HUSKELAPP_VERSJON failed, no rows affected.")
        }

        return idList.first()
    }

    private fun Connection.moveOppfolgingsoppgaveVersjon(oppfolgingsoppgaveId: Int) {
        val inserted = this.prepareStatement(MOVE_OPPFOLGINGSOPPGAVE_VERSJON_QUERY).use {
            it.setInt(1, oppfolgingsoppgaveId)
            it.executeUpdate()
        }
        if (inserted != 1) {
            throw NoElementInsertedException("Moving HUSKELAPP_VERSJON failed, no rows affected.")
        }
    }

    private fun Connection.removeOppfolgingsoppgaveVersjonLatest(oppfolgingsoppgaveId: Int) {
        val updated = this.prepareStatement(REMOVE_OPPFOLGINGSOPPGAVE_VERSJON_LATEST_QUERY).use {
            it.setInt(1, oppfolgingsoppgaveId)
            it.executeUpdate()
        }
        if (updated != 1) {
            throw NoElementInsertedException("Removing HUSKELAPP_VERSJON_LATEST failed, no rows affected.")
        }
    }

    override fun getUnpublished(): List<Oppfolgingsoppgave> =
        database.getUnpublishedOppfolgingsoppgaver().map { it.toOppfolgingsoppgave() }

    override fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave) =
        database.updatePublished(oppfolgingsoppgave = oppfolgingsoppgave)

    override fun updateRemovedOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave) =
        database.updateRemovedOppfolgingsoppgave(oppfolgingsoppgave = oppfolgingsoppgave)

    companion object {
        private const val CREATE_OPPFOLGINGSOPPGAVE_VERSJON_LATEST_QUERY =
            """
            INSERT INTO HUSKELAPP_VERSJON_LATEST (
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

        private const val MOVE_OPPFOLGINGSOPPGAVE_VERSJON_QUERY =
            """
            INSERT INTO HUSKELAPP_VERSJON (
                uuid,
                huskelapp_id,
                created_at,
                created_by,
                tekst,
                oppfolgingsgrunner,
                frist
            ) SELECT uuid,huskelapp_id,created_at,created_by,tekst,oppfolgingsgrunner,frist FROM HUSKELAPP_VERSJON_LATEST WHERE huskelapp_id = ?
            """

        private const val REMOVE_OPPFOLGINGSOPPGAVE_VERSJON_LATEST_QUERY =
            """
            DELETE FROM HUSKELAPP_VERSJON_LATEST WHERE huskelapp_id = ?
            """
    }
}

private const val queryCreateOppfolgingsoppgave =
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

private fun Connection.createOppfolgingsoppgave(
    oppfolgingsoppgave: Oppfolgingsoppgave,
): POppfolgingsoppgave {
    val idList = this.prepareStatement(queryCreateOppfolgingsoppgave).use {
        it.setString(1, oppfolgingsoppgave.uuid.toString())
        it.setString(2, oppfolgingsoppgave.personIdent.value)
        it.setObject(3, oppfolgingsoppgave.createdAt)
        it.setObject(4, oppfolgingsoppgave.updatedAt)
        it.setObject(5, oppfolgingsoppgave.isActive)
        it.executeQuery().toList { toPOppfolgingsoppgave() }
    }

    if (idList.size != 1) {
        throw NoElementInsertedException("Creating HUSKELAPP failed, no rows affected.")
    }

    return idList.first()
}

private const val queryUpdateOppfolgingsoppgaveUpdatedAt = """
    UPDATE huskelapp
    SET updated_at = ?
    WHERE uuid = ?
"""

private fun Connection.updateOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave) {
    this.prepareStatement(queryUpdateOppfolgingsoppgaveUpdatedAt).use {
        it.setObject(1, oppfolgingsoppgave.updatedAt)
        it.setString(2, oppfolgingsoppgave.uuid.toString())
        val updated = it.executeUpdate()
        if (updated != 1) {
            throw SQLException("Expected a single row to be updated, got update count $updated")
        }
    }
}

private const val queryGetOppfolgingsoppgaveByPersonIdent = """
    SELECT *
    FROM HUSKELAPP
    WHERE personident = ?
    ORDER BY created_at DESC
"""

private fun DatabaseInterface.getOppfolgingsoppgaver(personIdent: PersonIdent): List<POppfolgingsoppgave> {
    return connection.use { connection ->
        connection.prepareStatement(queryGetOppfolgingsoppgaveByPersonIdent).use {
            it.setString(1, personIdent.value)
            it.executeQuery().toList { toPOppfolgingsoppgave() }
        }
    }
}

private const val queryGetOppfolgingsoppgaveByUuid = """
    SELECT *
    FROM HUSKELAPP
    WHERE uuid = ?
"""

private fun DatabaseInterface.getPOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave? {
    return connection.use { connection ->
        connection.prepareStatement(queryGetOppfolgingsoppgaveByUuid).use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList { toPOppfolgingsoppgave() }.firstOrNull()
        }
    }
}

private const val queryGetActiveOppfolgingsoppgaverByPersonident = """
    SELECT h.*, hv.id as versjon_id, hv.uuid as versjon_uuid, hv.huskelapp_id as versjon_huskelapp_id, 
           hv.created_at as versjon_created_at, hv.created_by as versjon_created_by, hv.tekst as versjon_tekst, 
           hv.oppfolgingsgrunner as versjon_oppfolgingsgrunner, hv.frist as versjon_frist
    FROM HUSKELAPP h
    INNER JOIN HUSKELAPP_VERSJON_LATEST hv ON (h.id = hv.huskelapp_id)
    WHERE h.personident = ANY (string_to_array(?, ',')) AND h.is_active = true
"""

private fun DatabaseInterface.getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Oppfolgingsoppgave> {
    return if (personidenter.isEmpty()) {
        emptyList()
    } else {
        connection.use { connection ->
            connection.prepareStatement(queryGetActiveOppfolgingsoppgaverByPersonident).use { preparedStatement ->
                preparedStatement.setString(1, personidenter.joinToString(",") { it.value })
                preparedStatement.executeQuery().toList {
                    toPOppfolgingsoppgave().toOppfolgingsoppgave(
                        listOf(
                            toPOppfolgingsoppgaveVersjon(
                                col_name_prefix = "versjon_",
                            )
                        )
                    )
                }
            }
        }
    }
}

private const val queryGetOppfolgingsoppgaveVersjon = """
    SELECT *
    FROM HUSKELAPP_VERSJON
    WHERE huskelapp_id = ?
    ORDER BY created_at DESC
"""

private const val queryGetOppfolgingsoppgaveVersjonLatest = """
    SELECT *
    FROM HUSKELAPP_VERSJON_LATEST
    WHERE huskelapp_id = ?
    ORDER BY created_at DESC
"""

private fun DatabaseInterface.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon> {
    return connection.use { connection ->
        val latestVersion = connection.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId, latest = true)
        val oldVersions = connection.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId, latest = false)
        latestVersion + oldVersions
    }
}

internal fun Connection.getOppfolgingsoppgaveVersjoner(
    oppfolgingsoppgaveId: Int,
    latest: Boolean,
): List<POppfolgingsoppgaveVersjon> =
    this.prepareStatement(
        if (latest) queryGetOppfolgingsoppgaveVersjonLatest else queryGetOppfolgingsoppgaveVersjon
    ).use {
        it.setInt(1, oppfolgingsoppgaveId)
        it.executeQuery().toList { toPOppfolgingsoppgaveVersjon() }
    }

private const val queryGetUnpublishedOppfolgingsoppgaver = """
    SELECT * 
    FROM huskelapp 
    WHERE published_at IS NULL
    ORDER BY created_at ASC
"""

private fun DatabaseInterface.getUnpublishedOppfolgingsoppgaver(): List<POppfolgingsoppgave> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetUnpublishedOppfolgingsoppgaver).use {
            it.executeQuery().toList { toPOppfolgingsoppgave() }
        }
    }
}

private const val querySetPublished = """
    UPDATE huskelapp
    SET published_at = ?
    WHERE uuid = ?
"""

private fun DatabaseInterface.updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave) =
    this.connection.use { connection ->
        connection.updatePublished(oppfolgingsoppgave)
        connection.commit()
    }

private fun Connection.updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave) {
    this.prepareStatement(querySetPublished).use {
        it.setObject(1, oppfolgingsoppgave.publishedAt)
        it.setString(2, oppfolgingsoppgave.uuid.toString())
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

private fun DatabaseInterface.updateRemovedOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdateRemoved).use {
            it.setBoolean(1, oppfolgingsoppgave.isActive)
            it.setString(2, oppfolgingsoppgave.removedBy)
            it.setObject(3, oppfolgingsoppgave.publishedAt)
            it.setObject(4, oppfolgingsoppgave.updatedAt)
            it.setString(5, oppfolgingsoppgave.uuid.toString())
            val updated = it.executeUpdate()
            if (updated != 1) {
                throw SQLException("Expected a single row to be updated, got update count $updated")
            }
        }
        connection.commit()
    }
}

private fun ResultSet.toPOppfolgingsoppgave() = POppfolgingsoppgave(
    id = getInt("id"),
    uuid = UUID.fromString(getString("uuid")),
    personIdent = PersonIdent(getString("personident")),
    createdAt = getObject("created_at", OffsetDateTime::class.java),
    updatedAt = getObject("updated_at", OffsetDateTime::class.java),
    isActive = getBoolean("is_active"),
    publishedAt = getObject("published_at", OffsetDateTime::class.java),
    removedBy = getString("removed_by"),
)

private fun ResultSet.toPOppfolgingsoppgaveVersjon(
    col_name_prefix: String = "",
) = POppfolgingsoppgaveVersjon(
    id = getInt("${col_name_prefix}id"),
    uuid = UUID.fromString(getString("${col_name_prefix}uuid")),
    oppfolgingsoppgaveId = getInt("${col_name_prefix}huskelapp_id"),
    createdAt = getObject("${col_name_prefix}created_at", OffsetDateTime::class.java),
    createdBy = getString("${col_name_prefix}created_by"),
    tekst = getString("${col_name_prefix}tekst"),
    oppfolgingsgrunner = getString("${col_name_prefix}oppfolgingsgrunner").split(",").map(String::trim)
        .filter(String::isNotEmpty),
    frist = getDate("${col_name_prefix}frist")?.toLocalDate(),
)
