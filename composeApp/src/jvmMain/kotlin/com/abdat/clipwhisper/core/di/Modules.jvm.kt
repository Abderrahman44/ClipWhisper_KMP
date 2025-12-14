package com.abdat.clipwhisper.core.di

import com.abdat.clipwhisper.clipboard.domain.ClipboardManager
import com.abdat.clipwhisper.clipboard.presentation.ClipboardViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModuleDataBase = module {

    /*single<DatabaseDriverFactory> {
        DesktopDatabaseDriverFactory()
    }*/
    single { ClipboardManager() }
}

actual val ViewModelModules: Module = module {
    single { ClipboardViewModel(get()) }
}