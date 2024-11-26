package no.nav.syfo.infrastructure.database.repository.extension

import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Types

fun PreparedStatement.setStringOrNull(parameterIndex: Int, value: String?) {
    if (value == null) {
        this.setNull(parameterIndex, Types.LONGVARCHAR)
    } else {
        this.setString(parameterIndex, value)
    }
}

fun PreparedStatement.setDateOrNull(parameterIndex: Int, value: Date?) {
    if (value == null) {
        this.setNull(parameterIndex, Types.DATE)
    } else {
        this.setDate(parameterIndex, value)
    }
}
