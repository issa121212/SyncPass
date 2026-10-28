
package com.rojas.syncpass.data.remote

import com.rojas.syncpass.data.session.SessionStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun crear(
        baseUrl: String,
        sessionStore: SessionStore,
        accountKey: String
    ): SyncPassApi {

        val logging = HttpLoggingInterceptor().apply {
            level = if (com.rojas.syncpass.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->

                if (sessionStore.accountKey() != accountKey || !sessionStore.hasValidSession()) {
                    throw SessionInvalidIOException()
                }
                val token = sessionStore.currentAccessToken()

                val request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request()
                        .newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }

                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()



        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncPassApi::class.java)
    }
}
