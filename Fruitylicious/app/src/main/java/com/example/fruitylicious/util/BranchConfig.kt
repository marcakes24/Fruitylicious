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

    private val branchConfigData: BranchConfigData by lazy {
        loadBranchConfig()
    }

    val branchId: Int
        get() = branchConfigData.branchId

    val branchName: String
        get() = branchConfigData.branchName

    val apiBaseUrl: String
        get() = branchConfigData.apiBaseUrl

    val apiKey: String
        get() = branchConfigData.apiKey

    fun asData(): BranchConfigData {
        return branchConfigData
    }

    private fun loadBranchConfig(): BranchConfigData {
        val json = context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { reader ->
            reader.readText()
        }

        val jsonObject = JSONObject(json)

        val rawBaseUrl = jsonObject.getString("apiBaseUrl").trim()
        val normalizedBaseUrl = if (rawBaseUrl.endsWith("/")) {
            rawBaseUrl
        } else {
            "$rawBaseUrl/"
        }

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