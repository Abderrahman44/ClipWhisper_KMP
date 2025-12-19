package com.abdat.clipwhisper.network.di

import com.abdat.clipwhisper.network.data.DeviceDiscovery
import com.abdat.clipwhisper.network.data.DeviceInfoProvider
import com.abdat.clipwhisper.network.presentation.DeviceDiscoveryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual val NetworkModule = module {
    single { DeviceDiscovery(pairedStore = get()) }
    single { DeviceInfoProvider(androidContext()) }
    viewModelOf(::DeviceDiscoveryViewModel)
}