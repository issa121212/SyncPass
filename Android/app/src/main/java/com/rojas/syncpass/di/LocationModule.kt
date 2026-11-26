package com.rojas.syncpass.di

import com.rojas.syncpass.data.location.AndroidLocationSource
import com.rojas.syncpass.domain.LocationSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    abstract fun locationSource(source: AndroidLocationSource): LocationSource

    companion object {
        @Provides
        @Singleton
        fun clock(): Clock = Clock.systemUTC()
    }
}
