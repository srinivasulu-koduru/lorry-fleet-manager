package com.lorryowner.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lorryowner.app.ui.login.UserRole

/**
 * Manages authenticated user session persistence using EncryptedSharedPreferences
 * backed by Android Keystore.
 *
 * Requirements satisfied:
 * - Secure persistence of JWT bearer token and user metadata.
 * - Stores trusted backend role (OWNER or DRIVER).
 * - Automatic session recovery upon app relaunch.
 * - Complete session clearing on logout.
 */
class SessionManager(context: Context) {

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_FILE_NAME = "lorry_fleet_secure_session"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_LAST_WORKING_BASE_URL = "last_working_base_url"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
    }

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize EncryptedSharedPreferences with Keystore, falling back to standard prefs", e)
        // Fallback for emulators/devices where Keystore master key creation fails
        context.applicationContext.getSharedPreferences(PREFS_FILE_NAME + "_fallback", Context.MODE_PRIVATE)
    }

    /**
     * Saves authenticated user session details securely.
     */
    fun saveSession(
        token: String,
        role: String,
        username: String,
        userId: Long,
        fullName: String,
        expiresInMs: Long = 86400000L
    ) {
        val expiresAt = System.currentTimeMillis() + expiresInMs
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_ROLE, role.uppercase())
            .putString(KEY_USERNAME, username)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_FULL_NAME, fullName)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()

        Log.d(TAG, "Session securely saved for user '$username' with role '$role'")
    }

    /**
     * Retrieves the stored JWT access token, or null if absent/expired.
     */
    fun getAccessToken(): String? {
        val token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = sharedPreferences.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt > 0 && System.currentTimeMillis() >= expiresAt) {
            Log.d(TAG, "Stored access token has expired")
            clearSession()
            return null
        }
        return token
    }

    /**
     * Retrieves the trusted role returned by the backend.
     */
    fun getUserRole(): UserRole? {
        val roleStr = sharedPreferences.getString(KEY_USER_ROLE, null) ?: return null
        return when {
            roleStr.contains("OWNER") -> UserRole.OWNER
            roleStr.contains("DRIVER") -> UserRole.DRIVER
            else -> UserRole.UNSUPPORTED
        }
    }

    fun getUsername(): String {
        return sharedPreferences.getString(KEY_USERNAME, "") ?: ""
    }

    fun getFullName(): String {
        return sharedPreferences.getString(KEY_FULL_NAME, "") ?: ""
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong(KEY_USER_ID, -1L)
    }

    /**
     * Checks if an active, non-expired session exists.
     */
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null && getUserRole() != null
    }

    fun getLastWorkingBaseUrl(): String? {
        return sharedPreferences.getString(KEY_LAST_WORKING_BASE_URL, null)
    }

    fun saveLastWorkingBaseUrl(url: String) {
        sharedPreferences.edit().putString(KEY_LAST_WORKING_BASE_URL, url).apply()
    }

    fun getCustomBaseUrl(): String? {
        return sharedPreferences.getString(KEY_CUSTOM_BASE_URL, null)
    }

    fun saveCustomBaseUrl(url: String?) {
        if (url.isNullOrBlank()) {
            sharedPreferences.edit().remove(KEY_CUSTOM_BASE_URL).apply()
        } else {
            sharedPreferences.edit().putString(KEY_CUSTOM_BASE_URL, url.trim()).apply()
        }
    }

    /**
     * Clears all session credentials and tokens upon logout,
     * while preserving network preferences such as the last working backend URL and custom dev URL.
     */
    fun clearSession() {
        val lastUrl = getLastWorkingBaseUrl()
        val customUrl = getCustomBaseUrl()
        sharedPreferences.edit().clear().apply()
        if (lastUrl != null) {
            saveLastWorkingBaseUrl(lastUrl)
        }
        if (customUrl != null) {
            saveCustomBaseUrl(customUrl)
        }
        Log.d(TAG, "Session cleared")
    }
}
