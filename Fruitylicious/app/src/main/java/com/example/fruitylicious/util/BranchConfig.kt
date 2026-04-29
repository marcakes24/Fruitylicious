package com.example.fruitylicious.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class BranchConfigData(
    val branchId: Int,
    val branchName: String,
    val apiBaseUrl: String,
    val apiKey: String
)

@Singleton
class BranchConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val config: BranchConfigData by lazy {
        loadBranchConfig()
    }

    val branchId: Int
        get() = config.branchId

    val branchName: String
        get() = config.branchName

    val apiBaseUrl: String
        get() = config.apiBaseUrl

    val apiKey: String
        get() = config.apiKey

    fun getConfig(): BranchConfigData {
        return config
    }

    private fun loadBranchConfig(): BranchConfigData {
        val json = context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { reader ->
            reader.readText()
        }

        val jsonObject = JSONObject(json)

        val rawBaseUrl = jsonObject.getString("apiBaseUrl").trim()
        val normalizedBaseUrl = if (rawBaseUrl.endsWith("/")) rawBaseUrl else "$rawBaseUrl/"

        return BranchConfigData(
            branchId = jsonObject.getInt("branchId"),
            branchName = jsonObject.getString("branchName").trim(),
            apiBaseUrl = normalizedBaseUrl,
            apiKey = jsonObject.getString("apiKey").trim()
        )
    }

    companion object {
        private const val CONFIG_FILE_NAME = "branch_config.json"
    }
}