package com.abdat.clipwhisper.core.di

import com.abdat.clipwhisper.clipboard.domain.ClipboardManager
import com.abdat.clipwhisper.clipboard.presentation.ClipboardViewModel
import com.abdat.clipwhisper.settings.AppSettingsStore
import com.abdat.clipwhisper.settings.DesktopAppSettingsStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModuleDataBase = module {

    single { DatabaseDriverFactory() }
    single { ClipboardManager() }
    single<AppSettingsStore> { DesktopAppSettingsStore(get()) }
}

actual val ViewModelModules: Module = module {
    single { ClipboardViewModel(
        get(), get(),
        settingsStore = get()
    ) }
}