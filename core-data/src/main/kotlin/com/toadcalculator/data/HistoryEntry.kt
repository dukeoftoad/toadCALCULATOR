// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single history record: the expression as typed, the formatted result, and
 * the wall-clock timestamp in milliseconds. Failed expressions are never stored.
 */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expression: String,
    val result: String,
    val timestamp: Long,
)
