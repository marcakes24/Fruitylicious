package com.example.fruitylicious.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class BranchConfig(
    val branchId: Int,
    val branchName: String,
    val apiBaseUrl: String,
    val apiKey: String
)

@Singleton
class BranchConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    fun observeBranchConfig(): Flow<BranchConfig> = _configFlow.asStateFlow()

    val branchId: Int
        get() = _configFlow.value.branchId

    val branchName: String
        get() = _configFlow.value.branchName

    val apiBaseUrl: String
        get() = _configFlow.value.apiBaseUrl

    val apiKey: String
        get() = _configFlow.value.apiKey

    fun getBranchConfig(): BranchConfig = _configFlow.value

    fun saveBranchConfig(config: BranchConfig) {
        val normalizedUrl = if (config.apiBaseUrl.endsWith("/")) {
            config.apiBaseUrl
        } else {
            "${config.apiBaseUrl}/"
        }

        val updatedConfig = config.copy(apiBaseUrl = normalizedUrl)

        preferences.edit()
            .putInt(KEY_BRANCH_ID, updatedConfig.branchId)
            .putString(KEY_BRANCH_NAME, updatedConfig.branchName)
            .putString(KEY_API_BASE_URL, updatedConfig.apiBaseUrl)
            .putString(KEY_API_KEY, updatedConfig.apiKey)
            .apply()

        _configFlow.value = updatedConfig
    }

    fun resetBranchConfig() {
        preferences.edit()
            .remove(KEY_BRANCH_ID)
            .remove(KEY_BRANCH_NAME)
            .remove(KEY_API_BASE_URL)
            .remove(KEY_API_KEY)
            .apply()

        _configFlow.value = loadConfig()
    }

    private fun loadConfig(): BranchConfig {
        val savedId = preferences.getInt(KEY_BRANCH_ID, -1)
        return if (savedId != -1) {
            BranchConfig(
                branchId = savedId,
                branchName = preferences.getString(KEY_BRANCH_NAME, "") ?: "",
                apiBaseUrl = preferences.getString(KEY_API_BASE_URL, "") ?: "",
                apiKey = preferences.getString(KEY_API_KEY, "") ?: ""
            )
        } else {
            loadDefaultConfig()
        }
    }

    private fun loadDefaultConfig(): BranchConfig {
        return try {
            val json = context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val rawBaseUrl = jsonObject.getString("apiBaseUrl").trim()
            val normalizedBaseUrl = if (rawBaseUrl.endsWith("/")) rawBaseUrl else "$rawBaseUrl/"

            BranchConfig(
                branchId = jsonObject.getInt("branchId"),
                branchName = jsonObject.getString("branchName").trim(),
                apiBaseUrl = normalizedBaseUrl,
                apiKey = jsonObject.getString("apiKey").trim()
            )
        } catch (e: Exception) {
            // Fallback default
            BranchConfig(
                branchId = 1,
                branchName = "Branch 1",
                apiBaseUrl = "http://192.168.254.100:8083/",
                apiKey = "branch1-dev-api-key-secret"
            )
        }
    }

    companion object {
        private const val CONFIG_FILE_NAME = "branch_config.json"
        private const val PREF_NAME = "branch_config_prefs"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_BRANCH_NAME = "branch_name"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
    }
}
