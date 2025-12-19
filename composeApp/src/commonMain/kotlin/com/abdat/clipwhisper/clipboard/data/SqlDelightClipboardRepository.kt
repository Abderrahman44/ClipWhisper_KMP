package com.abdat.clipwhisper.clipboard.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abdat.clipwhisper.clipboard.domain.ClipboardRepository
import com.abdat.clipwhisper.clipboard.domain.models.ClipboardItem
import com.abdat.clipwhisper.core.di.DatabaseDriverFactory
import com.abdat.clipwhisper.db.ClipWhisperDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightClipboardRepository(
    driverFactory: DatabaseDriverFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ClipboardRepository {

    private val db = ClipWhisperDatabase(driverFactory.createDriver())
    private val q = db.clipWhisperQueries

    override fun observeRecent(limit: Long): Flow<List<ClipboardItem>> {
        return q.getRecentWithPinned(limit) // ✅ changed query
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows.map { r ->
                    ClipboardItem(
                        id = r.id,
                        type = r.type,
                        createdAtMillis = r.created_at_millis,
                        expiresAtMillis = r.expires_at_millis,
                        pinned = r.pinned,
                        originDeviceId = r.origin_device_id,
                        payload = r.payload
                    )
                }
            }
    }

    override suspend fun addToHistory(
        text: String,
        originDeviceId: String,
        nowMillis: Long,
        keepMax: Long,
        expiresAtMillis: Long?,
        pinned: Boolean
    ): Long = withContext(dispatcher) {
        db.transaction {
            // ✅ duplicates UX: remove only NON-PINNED duplicates
            q.deleteByPayloadNonPinned(text)

            q.insertItem(
                type = "TEXT",
                created_at_millis = nowMillis,
                expires_at_millis = expiresAtMillis,
                pinned = pinned,
                origin_device_id = originDeviceId,
                payload = text
            )

            // ✅ trim only NON-PINNED items
            q.deleteKeepNewestNNonPinned(keepMax)
        }
        q.lastInsertRowId().executeAsOne()
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) = withContext(dispatcher) {
        q.updatePinned(pinned = pinned, id = id)
    }

    override suspend fun deleteById(id: Long) = withContext(dispatcher) {
        q.deleteById(id)
    }

    override suspend fun deleteExpired(nowMillis: Long) = withContext(dispatcher) {
        q.deleteExpired(nowMillis)
    }

    override suspend fun clearAll(keepPinned: Boolean) = withContext(dispatcher) {
        if (keepPinned) q.deleteAllNonPinned() else q.deleteAllItems()
    }
}
