package com.prompthavenai.falcibuket.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The backend Cloud Functions are deployed in europe-west1. Android must use the
 * same region explicitly (the SDK default us-central1 would fail). This guards
 * the single source of truth used by [FirebaseBuketBackend.callable].
 */
class BackendConfigTest {

    @Test
    fun `functions region matches the deployed europe-west1 region`() {
        assertEquals("europe-west1", BackendConfig.FUNCTIONS_REGION)
    }

    @Test
    fun `functions region is not blank and has no surrounding whitespace`() {
        assertTrue(BackendConfig.FUNCTIONS_REGION.isNotBlank())
        assertEquals(BackendConfig.FUNCTIONS_REGION.trim(), BackendConfig.FUNCTIONS_REGION)
    }

    @Test
    fun `coffee callable timeout is explicit and above the function deadline`() {
        assertEquals(210L, BackendConfig.COFFEE_CALLABLE_TIMEOUT_SECONDS)
        assertTrue(BackendConfig.COFFEE_CALLABLE_TIMEOUT_SECONDS > 180L)
    }
}
