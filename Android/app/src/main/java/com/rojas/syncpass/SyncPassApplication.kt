package com.rojas.syncpass

import android.app.Application
import com.rojas.syncpass.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SyncPassApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncScheduler.programar(this)
    }
}
