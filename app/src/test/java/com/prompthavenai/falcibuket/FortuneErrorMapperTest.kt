package com.prompthavenai.falcibuket

import com.prompthavenai.falcibuket.data.remote.FortuneErrorMapper
import com.prompthavenai.falcibuket.data.remote.FortuneErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FortuneErrorMapperTest {

    @Test
    fun `detail code takes precedence over functions code`() {
        val code = FortuneErrorMapper.classify("INTERNAL", "IMAGE_TOO_LARGE")
        assertEquals(FortuneErrorCode.IMAGE_TOO_LARGE, code)
    }

    @Test
    fun `unauthenticated maps to auth error`() {
        assertEquals(FortuneErrorCode.AUTH_ERROR, FortuneErrorMapper.classify("UNAUTHENTICATED", null))
        assertEquals(FortuneErrorCode.AUTH_ERROR, FortuneErrorMapper.classify("PERMISSION_DENIED", null))
    }

    @Test
    fun `deadline maps to timeout`() {
        assertEquals(FortuneErrorCode.AI_TIMEOUT, FortuneErrorMapper.classify("DEADLINE_EXCEEDED", null))
    }

    @Test
    fun `resource exhausted maps to rate limit`() {
        assertEquals(FortuneErrorCode.AI_RATE_LIMIT, FortuneErrorMapper.classify("RESOURCE_EXHAUSTED", null))
    }

    @Test
    fun `unavailable and internal map to server error`() {
        assertEquals(FortuneErrorCode.AI_SERVER_ERROR, FortuneErrorMapper.classify("UNAVAILABLE", null))
        assertEquals(FortuneErrorCode.AI_SERVER_ERROR, FortuneErrorMapper.classify("INTERNAL", null))
    }

    @Test
    fun `no network overrides everything`() {
        val err = FortuneErrorMapper.fromThrowable(RuntimeException("boom"), networkAvailable = false)
        assertEquals(FortuneErrorCode.NO_NETWORK, err.code)
        assertTrue(err.userMessage.contains("İnternet"))
    }

    @Test
    fun `all codes have non blank user messages`() {
        FortuneErrorCode.values().forEach { code ->
            val msg = FortuneErrorMapper.userMessage(code)
            assertTrue("Missing message for $code", msg.isNotBlank())
        }
    }
}
