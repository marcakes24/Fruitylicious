package com.example.fruitylicious.data.remote.interceptor

import com.example.fruitylicious.util.BranchConfigManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val branchConfigManager: BranchConfigManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val config = branchConfigManager.getBranchConfig()
        
        val newBaseUrl = config.apiBaseUrl.toHttpUrlOrNull()
        
        if (newBaseUrl == null) {
            return chain.proceed(request)
        }

        val newUrl = request.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            // If the base URL has a path prefix, we might need to handle it here
            // but usually it's just host:port
            .build()

        val newRequest = request.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
