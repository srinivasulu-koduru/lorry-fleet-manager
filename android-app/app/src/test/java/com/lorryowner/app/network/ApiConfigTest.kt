package com.lorryowner.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiConfigTest {

    @Test
    fun testFormatBaseUrl_plainIp_appendsHttpAndPort() {
        val result = ApiConfig.formatBaseUrl("10.0.2.2")
        assertEquals("http://10.0.2.2:8080", result)
    }

    @Test
    fun testFormatBaseUrl_ipWithPort_prependsHttp() {
        val result = ApiConfig.formatBaseUrl("10.0.2.2:8080")
        assertEquals("http://10.0.2.2:8080", result)
    }

    @Test
    fun testFormatBaseUrl_fullUrlWithTrailingSlash_removesSlash() {
        val result = ApiConfig.formatBaseUrl("http://10.0.2.2:8080/")
        assertEquals("http://10.0.2.2:8080", result)
    }

    @Test
    fun testFormatBaseUrl_emptyInput_returnsEmpty() {
        val result = ApiConfig.formatBaseUrl("   ")
        assertEquals("", result)
    }

    @Test
    fun testEmulatorConstants_areCorrect() {
        assertEquals("10.0.2.2", ApiConfig.DEFAULT_EMULATOR_HOST)
        assertEquals("127.0.0.1", ApiConfig.DEFAULT_LOOPBACK_HOST)
        assertEquals("http://10.0.2.2:8080", ApiConfig.EMULATOR_BASE_URL)
        assertEquals("http://127.0.0.1:8080", ApiConfig.LOOPBACK_BASE_URL)
    }
}
