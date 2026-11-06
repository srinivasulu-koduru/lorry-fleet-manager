package com.lorryowner.app.network

import android.os.Build
import android.util.Log
import com.lorryowner.app.BuildConfig
import com.lorryowner.app.security.SessionManager

/**
 * Centralized Backend API & Network URL Configuration.
 *
 * Responsibilities:
 * 1. Production Backend Priority:
 *    - Defaults to the live, secure production backend:
 *      https://bhp-logistics-backend.getvoroa.com
 *    - Release builds strictly communicate exclusively with production over HTTPS.
 * 2. Environment-aware candidate URL resolution:
 *    - In production (release variant), only the production HTTPS endpoint is permitted.
 *    - In development (debug variant), the production endpoint is prioritized as default,
 *      while preserving fallback to local LAN or emulator for offline testing.
 * 3. Persistent working URL caching:
 *    - Once a connection succeeds, the working base URL is saved in secure storage
 *      so subsequent API calls across the entire application connect instantly without discovery latency.
 * 4. Consistent URL builder for all application endpoints.
 */
object ApiConfig {

    private const val TAG = "ApiConfig"

    /**
     * Deployed production backend base URL (Voroa Cloud Platform, TLS secured).
     */
    const val PROD_BASE_URL: String = "https://bhp-logistics-backend.getvoroa.com"

    val IS_PRODUCTION: Boolean = BuildConfig.IS_PRODUCTION

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
     * - In Production (Release build):
     *   Strictly uses the production HTTPS backend URL (no emulator, loopback, or cleartext IPs).
     *
     * - In Development (Debug build):
     *   1. Custom override URL (if explicitly configured by developer)
     *   2. Last working URL (if previously verified)
     *   3. PROD_BASE_URL (https://bhp-logistics-backend.getvoroa.com)
     *   4. Local emulator / LAN IP fallbacks (for offline testing)
     */
    fun getCandidateBaseUrls(sessionManager: SessionManager? = null): List<String> {
        val customUrl = sessionManager?.getCustomBaseUrl()
        val lastWorkingUrl = sessionManager?.getLastWorkingBaseUrl()

        if (IS_PRODUCTION) {
            // Production builds must only talk to production HTTPS backend
            return listOfNotNull(customUrl, PROD_BASE_URL)
                .map { formatBaseUrl(it) }
                .filter { it.isNotBlank() }
                .distinct()
        }

        val isEmu = isEmulator()
        Log.d(TAG, "Resolving base URLs: isEmulator=$isEmu, isProduction=$IS_PRODUCTION, customUrl=$customUrl, lastWorkingUrl=$lastWorkingUrl")

        val candidates = if (isEmu) {
            listOfNotNull(
                customUrl,
                lastWorkingUrl,
                PROD_BASE_URL,
                EMULATOR_BASE_URL,
                LOOPBACK_BASE_URL,
                DEV_LAN_BASE_URL
            )
        } else {
            listOfNotNull(
                customUrl,
                lastWorkingUrl,
                PROD_BASE_URL,
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
     * returns the production URL (or custom override if configured).
     */
    fun getBaseUrl(sessionManager: SessionManager? = null): String {
        val customUrl = sessionManager?.getCustomBaseUrl()
        if (!customUrl.isNullOrBlank()) {
            val formatted = formatBaseUrl(customUrl)
            activeBaseUrl = formatted
            return formatted
        }

        if (IS_PRODUCTION) {
            activeBaseUrl = PROD_BASE_URL
            return PROD_BASE_URL
        }

        activeBaseUrl?.let { return it }

        val lastSaved = sessionManager?.getLastWorkingBaseUrl()
        if (!lastSaved.isNullOrBlank()) {
            val formatted = formatBaseUrl(lastSaved)
            activeBaseUrl = formatted
            return formatted
        }

        // Default to production backend
        activeBaseUrl = PROD_BASE_URL
        return PROD_BASE_URL
    }

    /**
     * Returns the base URL guaranteed to end with a trailing slash.
     */
    fun getBaseUrlWithTrailingSlash(sessionManager: SessionManager? = null): String {
        return "${getBaseUrl(sessionManager).removeSuffix("/")}/"
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
     * Correctly handles standard HTTPS cloud domains without appending invalid port numbers.
     *
     * Accepts:
     * - "https://bhp-logistics-backend.getvoroa.com" -> "https://bhp-logistics-backend.getvoroa.com"
     * - "bhp-logistics-backend.getvoroa.com" -> "https://bhp-logistics-backend.getvoroa.com"
     * - "10.0.2.2" -> "http://10.0.2.2:8080"
     */
    fun formatBaseUrl(input: String): String {
        var trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Prepend scheme if missing; default to https for internet domains
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = if (trimmed.contains(".com") || trimmed.contains(".app") || trimmed.contains(".io") ||
                trimmed.contains(".net") || trimmed.contains(".org") || trimmed.contains("getvoroa")) {
                "https://$trimmed"
            } else {
                "http://$trimmed"
            }
        }

        trimmed = trimmed.removeSuffix("/")

        val scheme = trimmed.substringBefore("://")
        val withoutScheme = trimmed.substringAfter("://")
        val hostPart = withoutScheme.substringBefore("/")

        // Only append default port for HTTP when host has no port and is an IP or local host
        if (!hostPart.contains(":") && scheme.equals("http", ignoreCase = true)) {
            trimmed = "$scheme://$hostPart:$DEFAULT_PORT"
        }

        return trimmed
    }
}
