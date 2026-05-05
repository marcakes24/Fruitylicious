package com.example.fruitylicious.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        token: String,
        userId: Int,
        userName: String,
        username: String,
        role: String,
        branchId: Int,
        loginTime: Long
    ) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .putInt(KEY_BRANCH_ID, branchId)
            .putLong(KEY_LOGIN_TIME, loginTime)
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }

    fun updateActivity() {
        if (isLoggedIn()) {
            preferences.edit()
                .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
                .apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return getUserId() > 0 && !getRole().isNullOrBlank()
    }

    fun isSessionExpired(): Boolean {
        val lastActivity = preferences.getLong(KEY_LAST_ACTIVITY, 0L)

        if (lastActivity == 0L) {
            return true
        }

        val idleTime = System.currentTimeMillis() - lastActivity
        return idleTime >= SESSION_TIMEOUT_MILLIS
    }

    fun clearSession() {
        val lastPulledAt = getLastPulledAt()
        val lastSyncAt = getLastSyncAt()
        val lastSyncMessage = getLastSyncMessage()
        val lastSyncSuccessful = wasLastSyncSuccessful()

        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USERNAME)
            .remove(KEY_ROLE)
            .remove(KEY_BRANCH_ID)
            .remove(KEY_LOGIN_TIME)
            .remove(KEY_LAST_ACTIVITY)
            .putLong(KEY_LAST_PULLED_AT, lastPulledAt)
            .putLong(KEY_LAST_SYNC_AT, lastSyncAt)
            .putString(KEY_LAST_SYNC_MESSAGE, lastSyncMessage)
            .putBoolean(KEY_LAST_SYNC_SUCCESSFUL, lastSyncSuccessful)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString(KEY_TOKEN, null)
    }

    fun getUserId(): Int {
        return preferences.getInt(KEY_USER_ID, 0)
    }

    fun getUserName(): String {
        return preferences.getString(KEY_USER_NAME, "") ?: ""
    }

    fun getUsername(): String {
        return preferences.getString(KEY_USERNAME, "") ?: ""
    }

    fun getRole(): String? {
        return preferences.getString(KEY_ROLE, null)
    }

    fun getBranchId(): Int {
        return preferences.getInt(KEY_BRANCH_ID, 0)
    }

    fun getLoginTime(): Long {
        return preferences.getLong(KEY_LOGIN_TIME, 0L)
    }

    fun getLastActivity(): Long {
        return preferences.getLong(KEY_LAST_ACTIVITY, 0L)
    }

    fun saveLastPulledAt(timestamp: Long) {
        preferences.edit()
            .putLong(KEY_LAST_PULLED_AT, timestamp)
            .apply()
    }

    fun getLastPulledAt(): Long {
        return preferences.getLong(KEY_LAST_PULLED_AT, 0L)
    }

    fun saveSyncStatus(
        syncedAt: Long,
        success: Boolean,
        message: String
    ) {
        preferences.edit()
            .putLong(KEY_LAST_SYNC_AT, syncedAt)
            .putBoolean(KEY_LAST_SYNC_SUCCESSFUL, success)
            .putString(KEY_LAST_SYNC_MESSAGE, message)
            .apply()
    }

    fun getLastSyncAt(): Long {
        return preferences.getLong(KEY_LAST_SYNC_AT, 0L)
    }

    fun wasLastSyncSuccessful(): Boolean {
        return preferences.getBoolean(KEY_LAST_SYNC_SUCCESSFUL, false)
    }

    fun getLastSyncMessage(): String {
        return preferences.getString(KEY_LAST_SYNC_MESSAGE, "Not synced yet.") ?: "Not synced yet."
    }

    fun setPendingSync(pending: Boolean) {
        preferences.edit()
            .putBoolean(KEY_PENDING_SYNC, pending)
            .apply()
    }

    fun isPendingSync(): Boolean {
        return preferences.getBoolean(KEY_PENDING_SYNC, false)
    }

    companion object {
        private const val PREF_NAME = "fruitylicious_session"

        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_LAST_ACTIVITY = "last_activity"

        private const val KEY_LAST_PULLED_AT = "last_pulled_at"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_LAST_SYNC_SUCCESSFUL = "last_sync_successful"
        private const val KEY_LAST_SYNC_MESSAGE = "last_sync_message"
        private const val KEY_PENDING_SYNC = "pending_sync"

        private const val SESSION_TIMEOUT_MILLIS = 1L * 60L * 60L * 1000L
    }
}