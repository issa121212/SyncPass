package com.rojas.syncpass.di

import android.content.Context
import com.cognitoauth.CognitoAuthManager
import com.cognitoauth.CognitoConfig
import com.rojas.syncpass.core.config.CognitoAppConfig
import com.rojas.syncpass.data.session.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun sessionStore(@ApplicationContext context: Context): SessionStore = SessionStore(context)

    @Provides
    fun cognitoConfig(): CognitoConfig = CognitoAppConfig.config
}

@Module
@InstallIn(ViewModelComponent::class)
object CognitoModule {
    @Provides
    @ViewModelScoped
    fun authManager(@ApplicationContext context: Context, config: CognitoConfig): CognitoAuthManager =
        CognitoAuthManager(context, config)
}
