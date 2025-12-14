package com.abdat.clipwhisper.clipboard.domain

import app.cash.sqldelight.db.QueryResult
import com.abdat.clipwhisper.clipboard.domain.models.ClipboardItem
import kotlinx.coroutines.flow.Flow


interface ClipboardRepository {
    fun observeRecent(limit: Long = 10): Flow<List<ClipboardItem    >>
    suspend fun addToHistory(
        text: String,
        originDeviceId: String,
        nowMillis: Long,
        keepMax: Long = 10,
        expiresAtMillis: Long? = null,
        pinned: Boolean = false,
    ): Long

    suspend fun setPinned(id: Long, pinned: Boolean): QueryResult<Long>
    suspend fun deleteExpired(nowMillis: Long): QueryResult<Long>
    suspend fun clearAll(): QueryResult<Long>
}