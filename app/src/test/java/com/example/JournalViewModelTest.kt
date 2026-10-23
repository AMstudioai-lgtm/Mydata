package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.JournalRepository
import com.example.data.session.SessionProvider
import com.example.viewmodel.JournalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class JournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: JournalRepository
    private lateinit var viewModel: JournalViewModel

    private val noSession = object : SessionProvider {
        override fun currentUserId(): String? = null
        override fun currentEmail(): String? = null
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = JournalRepository(database, noSession)
        viewModel = JournalViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `renameJournal updates database name successfully`() = runTest(testDispatcher) {
        val journalId = repository.createJournal("Original Name")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.renameJournal(journalId, "Renamed Name")
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedJournals = repository.watchJournals(null).first()
        val journal = updatedJournals.find { it.id == journalId }
        assertEquals("Renamed Name", journal?.name)
    }

    @Test
    fun `renameJournal ignores blank name`() = runTest(testDispatcher) {
        val journalId = repository.createJournal("Original Name")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.renameJournal(journalId, "   ")
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedJournals = repository.watchJournals(null).first()
        val journal = updatedJournals.find { it.id == journalId }
        assertEquals("Original Name", journal?.name)
    }
}
