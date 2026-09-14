package com.prompthavenai.falcibuket.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoffeeAtlasRetryPolicyTest {

    @Test
    fun normalSuccessDoesNotRetry() {
        var retryCalls = 0
        val events = mutableListOf<String>()
        val run = CoffeeAtlasRetryPolicy.run(
            normal = { "normal" },
            retry = { retryCalls++; "retry" },
            onEvent = { events.add(it) }
        )
        assertEquals(CoffeeAtlasRetryPolicy.Outcome.NORMAL_OK, run.outcome)
        assertEquals("normal", run.value)
        assertEquals(0, retryCalls)
        assertTrue("no retry events", events.isEmpty())
    }

    @Test
    fun normalOomRetriesExactlyOnceAtLowerResolution() {
        var retryCalls = 0
        val events = mutableListOf<String>()
        val run = CoffeeAtlasRetryPolicy.run(
            normal = { throw OutOfMemoryError("boom") },
            retry = { retryCalls++; "retry" },
            onEvent = { events.add(it) }
        )
        assertEquals(CoffeeAtlasRetryPolicy.Outcome.RETRY_OK, run.outcome)
        assertEquals("retry", run.value)
        assertEquals(1, retryCalls)
        assertEquals(listOf(CoffeeAtlasRetryPolicy.EVENT_OOM_RETRY), events)
    }

    @Test
    fun secondOomReturnsControlledFailureWithNoThirdAttempt() {
        var normalCalls = 0
        var retryCalls = 0
        val events = mutableListOf<String>()
        val run = CoffeeAtlasRetryPolicy.run<String>(
            normal = { normalCalls++; throw OutOfMemoryError() },
            retry = { retryCalls++; throw OutOfMemoryError() },
            onEvent = { events.add(it) }
        )
        assertEquals(CoffeeAtlasRetryPolicy.Outcome.FINAL_FAILURE, run.outcome)
        assertNull(run.value)
        assertEquals(1, normalCalls)
        assertEquals(1, retryCalls)
        assertEquals(
            listOf(
                CoffeeAtlasRetryPolicy.EVENT_OOM_RETRY,
                CoffeeAtlasRetryPolicy.EVENT_OOM_FINAL_FAILURE
            ),
            events
        )
    }

    @Test
    fun ordinaryFailureDoesNotTriggerRetry() {
        var retryCalls = 0
        val outcome = runCatching {
            CoffeeAtlasRetryPolicy.run(
                normal = { throw IllegalStateException("not oom") },
                retry = { retryCalls++; "retry" },
                onEvent = {}
            )
        }
        assertTrue("ordinary exception propagates to caller", outcome.isFailure)
        assertEquals(0, retryCalls)
    }
}
