package com.abdat.clipwhisper.network.di

import com.abdat.clipwhisper.network.data.ClipboardSyncManager
import com.abdat.clipwhisper.network.data.DeviceDiscovery
import com.abdat.clipwhisper.network.data.DeviceDiscoveryManager
import com.abdat.clipwhisper.network.data.InMemoryPairedDeviceStore
import com.abdat.clipwhisper.network.data.tcp.TcpPairingManager
import com.abdat.clipwhisper.network.domain.PairedDeviceStore
import org.koin.core.module.Module
import org.koin.dsl.module

expect val NetworkModule: Module

val sharedNetworkModule = module {
    single<PairedDeviceStore> { InMemoryPairedDeviceStore() }
    single { DeviceDiscovery(pairedStore = get()) }
    single {
        DeviceDiscoveryManager(
            discovery = get(),
            pairedStore = get()
        )
    }
    single { ClipboardSyncManager(get(), get(), get(), get()) }

    single(createdAtStart = true) {
        TcpPairingManager(
            get(), get(),
            discoveryManager = get()
        ).apply {
            startServer()
        }
    }
}