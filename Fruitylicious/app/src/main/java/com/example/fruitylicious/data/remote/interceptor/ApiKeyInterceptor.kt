package com.example.fruitylicious.data.remote.interceptor

import com.example.fruitylicious.util.BranchConfigManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val branchConfigManager: BranchConfigManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader("X-API-KEY", branchConfigManager.apiKey)
            .addHeader("X-BRANCH-ID", branchConfigManager.branchId.toString())
            .build()

        return chain.proceed(newRequest)
    }
}