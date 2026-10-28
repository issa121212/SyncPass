package com.rojas.syncpass

import android.app.Application

import com.rojas.syncpass.data.local.SyncPassDatabase

import com.rojas.syncpass.data.remote.ApiClient

import com.rojas.syncpass.data.repository.SyncPassRepository

import com.rojas.syncpass.data.session.SessionStore

import com.rojas.syncpass.work.SyncScheduler

class SyncPassApplication : Application() {

    lateinit var container: AppContainer

        private set

    override fun onCreate() {

        super.onCreate()

        container = AppContainer(this)

        SyncScheduler.programar(this)

    }

}

class AppContainer(private val application: Application) {

    val sessionStore = SessionStore(application)

    private val repositories = mutableMapOf<String, SyncPassRepository>()

    val repository: SyncPassRepository

        @Synchronized get() {

            val account = requireNotNull(sessionStore.accountKey()) { "Inicia sesión nuevamente" }

            return repositories.getOrPut(account) {

                val database = SyncPassDatabase.obtener(application, account)

                val api = ApiClient.crear(

                    com.rojas.syncpass.core.config.AppConfig.apiBaseUrl, sessionStore, account)

                SyncPassRepository(api, database.dao(), sessionStore, database.recintoDao())

            }

        }

}

