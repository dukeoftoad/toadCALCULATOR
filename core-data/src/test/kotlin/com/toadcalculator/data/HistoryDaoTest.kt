// SPDX-License-Identifier: GPL-3.0-or-later

package com.toadcalculator.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HistoryDao
    private lateinit var repo: HistoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.historyDao()
        repo = RoomHistoryRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_observe_returns_newest_first() = runTest {
        repo.add(expression = "1+1", result = "2")
        Thread.sleep(5) // ensure distinct timestamps
        repo.add(expression = "2*2", result = "4")

        val entries = repo.observeAll().first()

        assertThat(entries).hasSize(2)
        assertThat(entries.first().expression).isEqualTo("2*2")
        assertThat(entries.last().expression).isEqualTo("1+1")
    }

    @Test
    fun delete_removes_single_entry() = runTest {
        repo.add(expression = "1+1", result = "2")
        val entry = repo.observeAll().first().single()

        repo.delete(entry)

        assertThat(repo.observeAll().first()).isEmpty()
    }

    @Test
    fun clearAll_removes_everything() = runTest {
        repo.add(expression = "1+1", result = "2")
        repo.add(expression = "3+3", result = "6")

        repo.clearAll()

        assertThat(repo.observeAll().first()).isEmpty()
        assertThat(dao.count()).isEqualTo(0)
    }

    @Test
    fun add_ignores_blank_expressions() = runTest {
        repo.add(expression = "   ", result = "0")

        assertThat(dao.count()).isEqualTo(0)
    }

    @Test
    fun entries_persist_fields() = runTest {
        repo.add(expression = "2^10", result = "1024")

        val entry = repo.observeAll().first().single()

        assertThat(entry.expression).isEqualTo("2^10")
        assertThat(entry.result).isEqualTo("1024")
        assertThat(entry.timestamp).isGreaterThan(0L)
        assertThat(entry.id).isGreaterThan(0L)
    }
}
