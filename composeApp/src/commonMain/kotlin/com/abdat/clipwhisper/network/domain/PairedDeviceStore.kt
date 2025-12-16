package com.abdat.clipwhisper.network.domain

import kotlinx.coroutines.flow.StateFlow

interface PairedDeviceStore {
    val approvedIds: StateFlow<Set<String>>

    suspend fun approve(deviceId: String)
    suspend fun revoke(deviceId: String)
    suspend fun isApproved(deviceId: String): Boolean
    suspend fun getApprovedIds(): Set<String>
}