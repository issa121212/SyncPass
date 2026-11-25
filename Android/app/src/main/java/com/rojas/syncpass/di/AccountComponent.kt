package com.rojas.syncpass.di

import android.content.Context
import com.rojas.syncpass.core.config.AppConfig
import com.rojas.syncpass.data.local.SyncPassDatabase
import com.rojas.syncpass.data.remote.ApiClient
import com.rojas.syncpass.data.remote.SyncPassApi
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.domain.SyncPassOperations
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.hilt.DefineComponent
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Scope
import javax.inject.Singleton

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class AccountScoped

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AccountKey

@AccountScoped
@DefineComponent(parent = SingletonComponent::class)
interface AccountComponent {
    @DefineComponent.Builder
    interface Builder {
        fun accountKey(@BindsInstance @AccountKey key: String): Builder
        fun build(): AccountComponent
    }
}

@EntryPoint
@InstallIn(AccountComponent::class)
interface AccountEntryPoint {
    fun operations(): SyncPassOperations
}

@Module
@InstallIn(AccountComponent::class)
object AccountModule {
    @Provides
    @AccountScoped
    fun database(@ApplicationContext context: Context, @AccountKey key: String): SyncPassDatabase =
        SyncPassDatabase.obtener(context, key)

    @Provides
    @AccountScoped
    fun api(sessionStore: SessionStore, @AccountKey key: String): SyncPassApi =
        ApiClient.crear(AppConfig.apiBaseUrl, sessionStore, key)

    @Provides
    @AccountScoped
    fun operations(api: SyncPassApi, database: SyncPassDatabase, sessionStore: SessionStore): SyncPassOperations =
        SyncPassRepository(api, database.dao(), sessionStore, database.recintoDao())
}

@Singleton
class AccountDependencies @Inject constructor(
    private val sessionStore: SessionStore,
    private val builders: Provider<AccountComponent.Builder>,
) {
    private val accounts = AccountScopeRegistry<SyncPassOperations> { key ->
        val component = builders.get().accountKey(key).build()
        EntryPoints.get(component, AccountEntryPoint::class.java).operations()
    }

    fun current(): SyncPassOperations {
        check(sessionStore.hasValidSession()) { "Inicia sesión nuevamente" }
        return accounts.get(requireNotNull(sessionStore.accountKey()) { "Inicia sesión nuevamente" })
    }

    fun clear() = accounts.clear()
}

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {
    @Provides
    @ViewModelScoped
    fun operations(accounts: AccountDependencies): SyncPassOperations = accounts.current()
}
