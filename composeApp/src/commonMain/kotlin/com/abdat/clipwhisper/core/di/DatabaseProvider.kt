package com.abdat.clipwhisper.core.di

import com.abdat.clipwhisper.db.ClipWhisperDatabase
import com.abdat.clipwhisper.core.di.DatabaseDriverFactory



class DatabaseProvider(
    private val driverFactory: DatabaseDriverFactory
) {

    val database: ClipWhisperDatabase by lazy {
        ClipWhisperDatabase(driverFactory.createDriver())
    }
}