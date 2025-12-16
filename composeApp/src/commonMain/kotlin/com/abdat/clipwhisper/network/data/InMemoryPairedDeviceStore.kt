package com.abdat.clipwhisper.network.data

import com.abdat.clipwhisper.network.domain.PairedDeviceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryPairedDeviceStore : PairedDeviceStore {
    private val mutex = Mutex()
    private val _approvedIds = MutableStateFlow<Set<String>>(emptySet())

    override val approvedIds: StateFlow<Set<String>> = _approvedIds.asStateFlow()

    override suspend fun approve(deviceId: String) {
        mutex.withLock {
            _approvedIds.value += deviceId
        }
    }

    override suspend fun revoke(deviceId: String) {
        mutex.withLock {
            _approvedIds.value -= deviceId
        }
    }

    override suspend fun isApproved(deviceId: String): Boolean =
        _approvedIds.value.contains(deviceId)

    override suspend fun getApprovedIds(): Set<String> =
        _approvedIds.value
}