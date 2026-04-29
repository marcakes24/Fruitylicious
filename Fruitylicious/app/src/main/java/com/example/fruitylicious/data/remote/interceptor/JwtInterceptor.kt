package com.example.fruitylicious.data.remote.interceptor

import com.example.fruitylicious.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.getToken()
        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}