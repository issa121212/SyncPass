package com.rojas.syncpass.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.di.AccountDependencies
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, SyncWorkerDependencies::class.java)
        if (!dependencies.sessionStore().hasValidSession()) return Result.success()
        val repository = try {
            dependencies.accounts().current()
        } catch (_: IllegalStateException) {
            return Result.success()
        }
        return repository.sincronizar().fold(
            onSuccess = { Result.success() },
            onFailure = { if ((it as? com.rojas.syncpass.domain.AppException)?.reintentable == true) Result.retry() else Result.failure() },
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerDependencies {
    fun sessionStore(): SessionStore
    fun accounts(): AccountDependencies
}

object SyncScheduler {

    private const val WORK_NAME = "syncpass-sync"

    fun programar(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun sincronizarAhora(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
