package com.lorryowner.app.network

import android.os.Build
import android.util.Log
import com.lorryowner.app.BuildConfig
import com.lorryowner.app.security.SessionManager

/**
 * Centralized Backend API & Network URL Configuration.
 *
 * Responsibilities:
 * 1. Environment-aware URL resolution:
 *    - Real physical phones prioritize local LAN Wi-Fi IP (e.g. http://10.231.41.81:8080).
 *    - Android Studio emulators prioritize QEMU router loopback (http://10.0.2.2:8080)
 *      and ADB reverse loopback (http://127.0.0.1:8080).
 * 2. Persistent working URL caching:
 *    - Once a connection succeeds, the working base URL is saved in secure storage
 *      so subsequent API calls across the entire application connect instantly without discovery latency.
 * 3. Configurable development host:
 *    - Default LAN host/port are injected at build time from build.gradle.kts / BuildConfig,
 *      and can also be customized dynamically at runtime.
 * 4. Consistent URL builder for all application endpoints.
 */
object ApiConfig {

    private const val TAG = "ApiConfig"

    val DEFAULT_PORT: Int = BuildConfig.DEV_BACKEND_PORT
    val DEFAULT_DEV_LAN_HOST: String = BuildConfig.DEV_BACKEND_HOST

    const val DEFAULT_EMULATOR_HOST: String = "10.0.2.2"
    const val DEFAULT_LOOPBACK_HOST: String = "127.0.0.1"

    val EMULATOR_BASE_URL: String = "http://$DEFAULT_EMULATOR_HOST:$DEFAULT_PORT"
    val LOOPBACK_BASE_URL: String = "http://$DEFAULT_LOOPBACK_HOST:$DEFAULT_PORT"
    val DEV_LAN_BASE_URL: String = "http://$DEFAULT_DEV_LAN_HOST:$DEFAULT_PORT"

    @Volatile
    private var activeBaseUrl: String? = null

    /**
     * Determines whether the current process is running inside an Android emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.FINGERPRINT.contains("sdk_")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.PRODUCT.contains("sdk")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    /**
     * Computes candidate base URLs ordered by probability of success for the current environment.
     *
     * - On a real physical phone:
     *   1. Custom override URL (if set by developer/user)
     *   2. Last working URL (persisted from previous successful request)
     *   3. DEV_LAN_BASE_URL (laptop Wi-Fi IPv4 address on the shared network)
     *   4. LOOPBACK_BASE_URL (http://127.0.0.1:8080, valid if connected via USB with adb reverse)
     *   5. EMULATOR_BASE_URL (http://10.0.2.2:8080, fallback)
     *
     * - On an Android Studio emulator:
     *   1. Custom override URL (if set)
     *   2. Last working URL
     *   3. EMULATOR_BASE_URL (http://10.0.2.2:8080)
     *   4. LOOPBACK_BASE_URL (http://127.0.0.1:8080)
     *   5. DEV_LAN_BASE_URL (http://10.231.41.81:8080)
     */
    fun getCandidateBaseUrls(sessionManager: SessionManager? = null): List<String> {
        val customUrl = sessionManager?.getCustomBaseUrl()
        val lastWorkingUrl = sessionManager?.getLastWorkingBaseUrl()
        val currentActive = activeBaseUrl

        val isEmu = isEmulator()
        Log.d(TAG, "Resolving base URLs: isEmulator=$isEmu, customUrl=$customUrl, lastWorkingUrl=$lastWorkingUrl")

        val candidates = if (isEmu) {
            listOfNotNull(
                customUrl,
                lastWorkingUrl,
                currentActive,
                EMULATOR_BASE_URL,
                LOOPBACK_BASE_URL,
                DEV_LAN_BASE_URL
            )
        } else {
            listOfNotNull(
                customUrl,
                lastWorkingUrl,
                currentActive,
                DEV_LAN_BASE_URL,
                LOOPBACK_BASE_URL,
                EMULATOR_BASE_URL
            )
        }

        return candidates
            .map { formatBaseUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * Returns the active base URL. If not yet determined by a successful call,
     * returns the most likely default candidate for the current device.
     */
    fun getBaseUrl(sessionManager: SessionManager? = null): String {
        activeBaseUrl?.let { return it }

        val customUrl = sessionManager?.getCustomBaseUrl()
        if (!customUrl.isNullOrBlank()) {
            val formatted = formatBaseUrl(customUrl)
            activeBaseUrl = formatted
            return formatted
        }

        val lastSaved = sessionManager?.getLastWorkingBaseUrl()
        if (!lastSaved.isNullOrBlank()) {
            val formatted = formatBaseUrl(lastSaved)
            activeBaseUrl = formatted
            return formatted
        }

        val fallback = if (isEmulator()) EMULATOR_BASE_URL else DEV_LAN_BASE_URL
        activeBaseUrl = fallback
        return fallback
    }

    /**
     * Updates the active base URL and persists it to secure storage when verified working.
     */
    fun onUrlSuccess(sessionManager: SessionManager?, verifiedUrl: String) {
        val formatted = formatBaseUrl(verifiedUrl)
        activeBaseUrl = formatted
        sessionManager?.saveLastWorkingBaseUrl(formatted)
        Log.i(TAG, "Backend base URL verified and locked to: $formatted")
    }

    /**
     * Manually updates the development host IP or URL at runtime.
     */
    fun setCustomDevUrl(sessionManager: SessionManager?, inputUrl: String?) {
        if (inputUrl.isNullOrBlank()) {
            sessionManager?.saveCustomBaseUrl(null)
            activeBaseUrl = null
        } else {
            val formatted = formatBaseUrl(inputUrl)
            activeBaseUrl = formatted
            sessionManager?.saveCustomBaseUrl(formatted)
            sessionManager?.saveLastWorkingBaseUrl(formatted)
            Log.i(TAG, "Custom backend URL set: $formatted")
        }
    }

    /**
     * Builds a full endpoint URL using the currently selected backend base URL.
     * Guarantees consistency across all API calls in the application.
     */
    fun buildUrl(path: String, sessionManager: SessionManager? = null): String {
        val base = getBaseUrl(sessionManager).removeSuffix("/")
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$base$cleanPath"
    }

    /**
     * Normalizes a host/URL string into a valid HTTP/HTTPS base URL.
     * Accepts:
     * - "10.231.41.81" -> "http://10.231.41.81:8080"
     * - "10.231.41.81:8080" -> "http://10.231.41.81:8080"
     * - "http://10.231.41.81:8080/" -> "http://10.231.41.81:8080"
     */
    fun formatBaseUrl(input: String): String {
        var trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Prepend scheme if missing
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = "http://$trimmed"
        }

        trimmed = trimmed.removeSuffix("/")

        // Check if port is specified after the host
        val scheme = trimmed.substringBefore("://")
        val withoutScheme = trimmed.substringAfter("://")
        val hostPart = withoutScheme.substringBefore("/")

        if (!hostPart.contains(":")) {
            trimmed = "$scheme://$hostPart:$DEFAULT_PORT"
        }

        return trimmed
    }
}
