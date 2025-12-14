package com.abdat.clipwhisper.core.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.abdat.clipwhisper.db.ClipWhisperDatabase
import java.io.File
import java.util.Properties

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:ClipWhisper.db", Properties(),
            ClipWhisperDatabase.Schema)
        return driver
    }
}