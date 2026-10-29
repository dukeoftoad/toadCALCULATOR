// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.data

import kotlinx.coroutines.flow.Flow

/**
 * Storage-agnostic history API so ViewModels can be tested without Room.
 */
interface HistoryRepository {
    /** All entries, newest first. */
    fun observeAll(): Flow<List<HistoryEntry>>

    /** Persists one successful evaluation. */
    suspend fun add(expression: String, result: String)

    /** Deletes a single entry. */
    suspend fun delete(entry: HistoryEntry)

    /** Deletes every entry. */
    suspend fun clearAll()
}

class RoomHistoryRepository(
    private val dao: HistoryDao,
) : HistoryRepository {
    override fun observeAll(): Flow<List<HistoryEntry>> = dao.observeAll()

    override suspend fun add(expression: String, result: String) {
        if (expression.isBlank()) return
        dao.insert(HistoryEntry(expression = expression, result = result, timestamp = System.currentTimeMillis()))
    }

    override suspend fun delete(entry: HistoryEntry) = dao.delete(entry)

    override suspend fun clearAll() = dao.clearAll()
}
