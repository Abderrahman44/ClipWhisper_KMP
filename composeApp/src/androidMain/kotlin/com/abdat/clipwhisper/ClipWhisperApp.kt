package com.abdat.clipwhisper

import android.app.Application
import com.abdat.clipwhisper.core.di.ViewModelModules
import com.abdat.clipwhisper.core.di.initKoin
import com.abdat.clipwhisper.core.di.platformModuleDataBase
import com.abdat.clipwhisper.core.di.sharedDBModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger


class ClipWhisperApp :  Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@ClipWhisperApp)
            modules(
                platformModuleDataBase,
                sharedDBModules,
                ViewModelModules

            )
        }
    }
}