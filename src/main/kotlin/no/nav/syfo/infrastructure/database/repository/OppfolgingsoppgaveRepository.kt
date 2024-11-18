package no.nav.syfo.infrastructure.database.repository

import IOppfolgingsoppgaveRepository
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Oppfolgingsoppgave
import no.nav.syfo.domain.OppfolgingsoppgaveHistorikk
import no.nav.syfo.infrastructure.COUNT_HUSKELAPP_VERSJON_CREATED
import no.nav.syfo.infrastructure.database.*
import no.nav.syfo.util.nowUTC
import java.sql.*
import java.sql.Date
import java.time.OffsetDateTime
import java.util.*

class OppfolgingsoppgaveRepository(
    private val database: DatabaseInterface,
) : IOppfolgingsoppgaveRepository {
    override fun getOppfolgingsoppgaver(personIdent: PersonIdent): List<POppfolgingsoppgave> {
        return database.getOppfolgingsoppgaver(personIdent)
    }

    override fun getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Pair<POppfolgingsoppgave, POppfolgingsoppgaveVersjon>> =
        database.getActiveOppfolgingsoppgaver(personidenter)

    override fun getOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave? {
        return database.getOppfolgingsoppgave(uuid)
    }

    override fun getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon> {
        return database.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId)
    }

    override fun create(oppfolgingsoppgave: Oppfolgingsoppgave): Oppfolgingsoppgave {
        database.connection.use { connection ->
            val createdOppfolgingsoppgave = connection.createOppfolgingsoppgave(oppfolgingsoppgave)
            val createdVersion = connection.createOppfolgingsoppgaveVersjon(
                oppfolgingsoppgaveId = createdOppfolgingsoppgave.id,
                newOppfolgingsoppgaveVersion = oppfolgingsoppgave,
            )
            connection.commit()
            return createdOppfolgingsoppgave.toOppfolgingsoppgave(createdVersion)
        }
    }

    override fun create(oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk): OppfolgingsoppgaveHistorikk {
        database.connection.use { connection ->
            val createdOppfolgingsoppgave = connection.createOppfolgingsoppgaveHistorikk(oppfolgingsoppgaveHistorikk)
            val createdVersion = connection.createOppfolgingsoppgaveVersjon(
                oppfolgingsoppgaveId = createdOppfolgingsoppgave.id,
                newOppfolgingsoppgaveHistorikk = oppfolgingsoppgaveHistorikk
            )
            connection.commit()
            return createdOppfolgingsoppgave.toOppfolgingsoppgaveHistorikk(listOf(createdVersion))
        }
    }

    override fun createVersion(
        oppfolgingsoppgaveId: Int,
        newOppfolgingsoppgaveVersion: Oppfolgingsoppgave,
    ): POppfolgingsoppgaveVersjon =
        database.connection.use { connection ->
            val oppfolgingsoppgaveVersjon = connection.createOppfolgingsoppgaveVersjon(
                oppfolgingsoppgaveId = oppfolgingsoppgaveId,
                newOppfolgingsoppgaveVersion = newOppfolgingsoppgaveVersion,
            )
            connection.updatePublished(oppfolgingsoppgave = newOppfolgingsoppgaveVersion)
            connection.commit()
            return oppfolgingsoppgaveVersjon
        }

    override fun edit(
        oppfolgingsoppgaveId: Int,
        oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk
    ): OppfolgingsoppgaveHistorikk {
        database.connection.use { connection ->
            connection.createOppfolgingsoppgaveVersjon(oppfolgingsoppgaveId, oppfolgingsoppgaveHistorikk)
            connection.updateOppfolgingsoppgave(oppfolgingsoppgaveHistorikk)
            connection.commit()
            COUNT_HUSKELAPP_VERSJON_CREATED.increment()
            return oppfolgingsoppgaveHistorikk
        }
    }

    private fun Connection.createOppfolgingsoppgaveVersjon(
        oppfolgingsoppgaveId: Int,
        newOppfolgingsoppgaveVersion: Oppfolgingsoppgave,
    ): POppfolgingsoppgaveVersjon {
        val idList = this.prepareStatement(CREATE_OPPFOLGINGSOPPGAVE_VERSJON_QUERY).use {
            it.setString(1, UUID.randomUUID().toString())
            it.setInt(2, oppfolgingsoppgaveId)
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
            it.executeQuery().toList { toPOppfolgingsoppgaveVersjon() }
        }

        if (idList.size != 1) {
            throw NoElementInsertedException("Creating HUSKELAPP_VERSJON failed, no rows affected.")
        }

        return idList.first()
    }

    private fun Connection.createOppfolgingsoppgaveVersjon(
        oppfolgingsoppgaveId: Int,
        newOppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk
    ): POppfolgingsoppgaveVersjon {
        //TODO: Hva bør denne metoden ta inn? Oppgaven eller versjonen?
        val newVersjon = newOppfolgingsoppgaveHistorikk.versjoner.first()
        val idList = this.prepareStatement(CREATE_OPPFOLGINGSOPPGAVE_VERSJON_QUERY).use {
            it.setString(1, UUID.randomUUID().toString())
            it.setInt(2, oppfolgingsoppgaveId)
            it.setObject(3, newVersjon.createdAt)
            it.setString(4, newVersjon.createdBy)
            it.setStringOrNull(5, newVersjon.tekst)
            it.setStringOrNull(6, newVersjon.oppfolgingsgrunn.toString())
            //TODO: Endre denne til noe annet enn it mtp. shadowing?
            it.setDateOrNull(7, newVersjon.frist?.let { Date.valueOf(it) })
            it.executeQuery().toList { toPOppfolgingsoppgaveVersjon() }
        }

        if (idList.size != 1) {
            throw NoElementInsertedException("Creating HUSKELAPP_VERSJON failed, no rows affected.")
        }

        return idList.first()
    }

    //TODO: Flytte? I så fall hvor?
    private fun PreparedStatement.setStringOrNull(parameterIndex: Int, value: String?) {
        if (value == null) {
            this.setNull(parameterIndex, Types.LONGVARCHAR)
        } else {
            this.setString(parameterIndex, value)
        }
    }

    //TODO: Flytte? I så fall hvor?
    private fun PreparedStatement.setDateOrNull(parameterIndex: Int, value: Date?) {
        if (value == null) {
            this.setNull(parameterIndex, Types.DATE)
        } else {
            this.setDate(parameterIndex, value)
        }
    }

    override fun getUnpublished(): List<POppfolgingsoppgave> = database.getUnpublishedOppfolgingsoppgaver()
    override fun updatePublished(oppfolgingsoppgave: Oppfolgingsoppgave) =
        database.updatePublished(oppfolgingsoppgave = oppfolgingsoppgave)

    override fun updateRemovedOppfolgingsoppgave(oppfolgingsoppgave: Oppfolgingsoppgave) =
        database.updateRemovedOppfolgingsoppgave(oppfolgingsoppgave = oppfolgingsoppgave)

    companion object {
        private const val CREATE_OPPFOLGINGSOPPGAVE_VERSJON_QUERY =
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
    val now = nowUTC()
    val idList = this.prepareStatement(queryCreateOppfolgingsoppgave).use {
        it.setString(1, oppfolgingsoppgave.uuid.toString())
        it.setString(2, oppfolgingsoppgave.personIdent.value)
        it.setObject(3, now)
        it.setObject(4, now)
        it.setObject(5, oppfolgingsoppgave.isActive)
        it.executeQuery().toList { toPOppfolgingsoppgave() }
    }

    if (idList.size != 1) {
        throw NoElementInsertedException("Creating HUSKELAPP failed, no rows affected.")
    }

    return idList.first()
}

private fun Connection.createOppfolgingsoppgaveHistorikk(
    oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk,
): POppfolgingsoppgave {
    val idList = this.prepareStatement(queryCreateOppfolgingsoppgave).use {
        it.setString(1, oppfolgingsoppgaveHistorikk.uuid.toString())
        it.setString(2, oppfolgingsoppgaveHistorikk.personIdent.value)
        it.setObject(3, oppfolgingsoppgaveHistorikk.createdAt)
        it.setObject(4, oppfolgingsoppgaveHistorikk.updatedAt)
        it.setObject(5, oppfolgingsoppgaveHistorikk.isActive)
        it.executeQuery().toList { toPOppfolgingsoppgave() }
    }

    if (idList.size != 1) {
        throw NoElementInsertedException("Creating HUSKELAPP failed, no rows affected.")
    }

    return idList.first()
}

//TODO: Rename
private const val queryUpdatedAt = """
    UPDATE huskelapp
    SET updated_at = ?
    WHERE uuid = ?
"""

private fun Connection.updateOppfolgingsoppgave(oppfolgingsoppgaveHistorikk: OppfolgingsoppgaveHistorikk) {
    this.prepareStatement(queryUpdatedAt).use {
        it.setObject(1, oppfolgingsoppgaveHistorikk.updatedAt)
        it.setString(2, oppfolgingsoppgaveHistorikk.uuid.toString())
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

private fun DatabaseInterface.getOppfolgingsoppgave(uuid: UUID): POppfolgingsoppgave? {
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
    INNER JOIN (
        SELECT hv1.*
        FROM HUSKELAPP_VERSJON hv1
        INNER JOIN (
            SELECT huskelapp_id, MAX(created_at) AS max_created_at
            FROM HUSKELAPP_VERSJON
            GROUP BY huskelapp_id
        ) hv2 ON hv1.huskelapp_id = hv2.huskelapp_id AND hv1.created_at = hv2.max_created_at
    ) hv ON h.id = hv.huskelapp_id
    WHERE h.personident = ANY (string_to_array(?, ',')) AND h.is_active = true
"""

private fun DatabaseInterface.getActiveOppfolgingsoppgaver(personidenter: List<PersonIdent>): List<Pair<POppfolgingsoppgave, POppfolgingsoppgaveVersjon>> {
    return if (personidenter.isEmpty()) {
        emptyList()
    } else {
        connection.use { connection ->
            connection.prepareStatement(queryGetActiveOppfolgingsoppgaverByPersonident).use { preparedStatement ->
                preparedStatement.setString(1, personidenter.joinToString(",") { it.value })
                preparedStatement.executeQuery().toList {
                    Pair(
                        toPOppfolgingsoppgave(),
                        toPOppfolgingsoppgaveVersjon(
                            col_name_prefix = "versjon_",
                        ),
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

private fun DatabaseInterface.getOppfolgingsoppgaveVersjoner(oppfolgingsoppgaveId: Int): List<POppfolgingsoppgaveVersjon> {
    return connection.use { connection ->
        connection.prepareStatement(queryGetOppfolgingsoppgaveVersjon).use {
            it.setInt(1, oppfolgingsoppgaveId)
            it.executeQuery().toList { toPOppfolgingsoppgaveVersjon() }
        }
    }
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
    SET published_at = ?, updated_at = ?
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
        it.setObject(2, oppfolgingsoppgave.updatedAt)
        it.setString(3, oppfolgingsoppgave.uuid.toString())
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
    oppfolgingsgrunner = getString("${col_name_prefix}oppfolgingsgrunner").split(",").map(String::trim).filter(String::isNotEmpty),
    frist = getDate("${col_name_prefix}frist")?.toLocalDate(),
)
