package com.abdat.clipwhisper.core.di

import com.abdat.clipwhisper.clipboard.data.SqlDelightClipboardRepository
import com.abdat.clipwhisper.clipboard.domain.ClipboardRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

expect val platformModuleDataBase : Module
val sharedDBModules = module {
    single<Clock> { Clock.System }
    single { DatabaseProvider(get()) }
    single<ClipboardRepository> { SqlDelightClipboardRepository(get()) }


}

expect val ViewModelModules : Module