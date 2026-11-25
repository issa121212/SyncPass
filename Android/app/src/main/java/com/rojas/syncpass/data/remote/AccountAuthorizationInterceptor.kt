package com.rojas.syncpass.data.remote

import com.rojas.syncpass.data.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

internal class AccountAuthorizationInterceptor(
    private val sessionStore: SessionStore,
    private val accountKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStore.accessTokenFor(accountKey) ?: throw SessionInvalidIOException()
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
