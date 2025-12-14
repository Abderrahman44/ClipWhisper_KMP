package com.abdat.clipwhisper.core.di

import app.cash.sqldelight.db.SqlDriver
import com.abdat.clipwhisper.db.ClipWhisperDatabase

/**
 * Platform-specific database driver factory
 * Implementations provided in androidMain and desktopMain
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): ClipWhisperDatabase {
    val driver = driverFactory.createDriver()
    val database = ClipWhisperDatabase(driver)

    // Do more work with the database (see below).

    return database;
}