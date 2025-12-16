package com.abdat.clipwhisper.core.di

import com.abdat.clipwhisper.network.di.NetworkModule
import com.abdat.clipwhisper.network.di.sharedNetworkModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            platformModuleDataBase,
            sharedDBModules,
            ViewModelModules,
            NetworkModule,
            sharedNetworkModule
        )
    }
}