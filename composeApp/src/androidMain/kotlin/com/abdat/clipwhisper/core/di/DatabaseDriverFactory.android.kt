package com.abdat.clipwhisper.core.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.abdat.clipwhisper.db.ClipWhisperDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(ClipWhisperDatabase.Schema, context, "ClipWhisper.db")
    }
}