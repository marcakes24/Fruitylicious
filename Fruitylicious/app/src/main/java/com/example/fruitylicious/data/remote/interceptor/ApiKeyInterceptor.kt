package com.example.fruitylicious.data.remote.interceptor

import com.example.fruitylicious.util.BranchConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val branchConfig: BranchConfig
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader("X-API-KEY", branchConfig.apiKey)
            .addHeader("X-BRANCH-ID", branchConfig.branchId.toString())
            .build()

        return chain.proceed(newRequest)
    }
}