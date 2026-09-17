package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.remote.socket.SocketManager
import com.example.data.repository.GroupTrackRepository

class GroupTrackApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var repository: GroupTrackRepository
        private set

    lateinit var socketManager: SocketManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        sessionManager = SessionManager(this)
        repository = GroupTrackRepository(this, sessionManager, database)
        socketManager = SocketManager(sessionManager)
    }
}
