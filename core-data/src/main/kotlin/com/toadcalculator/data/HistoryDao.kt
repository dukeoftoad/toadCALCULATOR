// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    /** Newest first: timestamp descending, then auto-id descending for ties. */
    @Query("SELECT * FROM history ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<HistoryEntry>>

    @Insert
    suspend fun insert(entry: HistoryEntry): Long

    @Delete
    suspend fun delete(entry: HistoryEntry)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}
