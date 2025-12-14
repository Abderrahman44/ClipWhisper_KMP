package com.abdat.clipwhisper.core.di

import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

expect val platformModuleDataBase : Module
val sharedDBModules = module {
    single<Clock> { Clock.System }
    //single { DatabaseProvider(get()) }

}

expect val ViewModelModules : Module