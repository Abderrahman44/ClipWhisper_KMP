package com.abdat.clipwhisper.core.di


import com.abdat.clipwhisper.clipboard.domain.ClipboardManager
import com.abdat.clipwhisper.clipboard.presentation.ClipboardViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


actual val platformModuleDataBase: Module = module {

    single { ClipboardManager(androidContext()) }

}
actual val ViewModelModules: Module = module {
    viewModelOf(::ClipboardViewModel)
}