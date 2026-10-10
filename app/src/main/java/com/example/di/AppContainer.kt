package com.example.di

import android.content.Context
import com.example.data.auth.SupabaseAuthService
import com.example.data.db.AppDatabase
import com.example.data.repository.JournalRepository

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)
    val journalRepository: JournalRepository = JournalRepository(database)
    val authService: SupabaseAuthService = SupabaseAuthService(context)
}
