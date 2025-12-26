package com.abdat.clipwhisper.core.di


import com.abdat.clipwhisper.clipboard.domain.ClipboardManager
import com.abdat.clipwhisper.clipboard.presentation.ClipboardViewModel
import com.abdat.clipwhisper.settings.AndroidAppSettingsStore
import com.abdat.clipwhisper.settings.AppSettingsStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


actual val platformModuleDataBase: Module = module {

    single { DatabaseDriverFactory(androidContext()) }
    single { ClipboardManager(androidContext()) }
    single<AppSettingsStore> { AndroidAppSettingsStore(androidContext(),get()) }

}
actual val ViewModelModules: Module = module {
    viewModel { ClipboardViewModel(
        sync = get(),
        repo = get(),
        settingsStore = get(),
    ) }
}