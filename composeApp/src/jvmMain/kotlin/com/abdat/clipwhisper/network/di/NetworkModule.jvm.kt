package com.abdat.clipwhisper.network.di

import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.presentation.DeviceDiscoveryViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val NetworkModule = module {
    single { DeviceInfoProvider() }
    singleOf(::DeviceDiscoveryViewModel)
}