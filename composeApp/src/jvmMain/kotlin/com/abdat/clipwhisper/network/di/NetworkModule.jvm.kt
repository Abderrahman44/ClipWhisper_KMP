package com.abdat.clipwhisper.network.di

import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.presentation.DeviceDiscoveryViewModel
import org.koin.dsl.module

actual val NetworkModule = module {
    single { DeviceInfoProvider() }
    single { DeviceDiscoveryViewModel(get(),get()) }

}