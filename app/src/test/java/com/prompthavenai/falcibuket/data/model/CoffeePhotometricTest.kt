package com.prompthavenai.falcibuket.data.model

import com.prompthavenai.falcibuket.data.model.CoffeeCanonicalReconstruction as C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoffeePhotometricTest {

    private val size = 64

    private fun uniformDisk(value: Int): C.CanonicalDisk {
        val argb = IntArray(size * size) { value }
        val valid = BooleanArray(size * size) { true }
        return C.CanonicalDisk(C.PixelBuffer(size, size, argb), valid, size)
    }

    private fun uniformDiskRgb(r: Int, g: Int, b: Int): C.CanonicalDisk =
        uniformDisk(C.argb(r, g, b))

    @Test
    fun beneficialCorrectionIsAccepted() {
        val anchor = uniformDiskRgb(200, 200, 200)
        val source = uniformDiskRgb(160, 160, 160)
        val candidate = CoffeePhotometric.estimateCandidate(anchor, source)
        val decision = CoffeePhotometric.evaluateAndGate(anchor, source, candidate)
        assertTrue("decision ${decision.status}", decision.accepted)
        assertTrue(decision.after < decision.before)
        assertTrue(decision.samples > 0)
    }

    @Test
    fun harmfulCorrectionIsRejected() {
        val anchor = uniformDiskRgb(200, 200, 200)
        val source = uniformDiskRgb(160, 160, 160)
        val harmful = CoffeePhotometric.Correction(
            floatArrayOf(0.85f, 0.85f, 0.85f),
            floatArrayOf(0f, 0f, 0f)
        )
        val decision = CoffeePhotometric.evaluateAndGate(anchor, source, harmful)
        assertFalse("must reject worsening correction", decision.accepted)
        assertEquals(CoffeePhotometric.Decision.Status.REJECTED, decision.status)
    }

    @Test
    fun identityIsRejectedWhenNoImprovement() {
        val anchor = uniformDiskRgb(180, 180, 180)
        val decision = CoffeePhotometric.evaluateAndGate(anchor, anchor, CoffeePhotometric.Correction.IDENTITY)
        assertFalse(decision.accepted)
    }

    @Test
    fun gainsAreBounded() {
        val anchor = uniformDiskRgb(255, 255, 255)
        val source = uniformDiskRgb(10, 10, 10)
        val candidate = CoffeePhotometric.estimateCandidate(anchor, source)
        for (g in candidate.gain) {
            assertTrue("gain $g", g in CoffeePhotometric.MIN_GAIN..CoffeePhotometric.MAX_GAIN)
        }
    }

    @Test
    fun applyToDiskChangesValidPixelsOnly() {
        val argb = IntArray(size * size) { C.argb(160, 160, 160) }
        val valid = BooleanArray(size * size) { it % 2 == 0 }
        val source = C.CanonicalDisk(C.PixelBuffer(size, size, argb), valid, size)
        val corrected = CoffeePhotometric.applyToDisk(
            source,
            CoffeePhotometric.Correction(floatArrayOf(1.1f, 1.1f, 1.1f), floatArrayOf(0f, 0f, 0f))
        )
        for (i in valid.indices) {
            if (valid[i]) {
                assertTrue("valid pixel must brighten", ((corrected.image.argb[i] ushr 16) and 0xFF) > 160)
            } else {
                assertEquals("invalid pixel must stay untouched", source.image.argb[i], corrected.image.argb[i])
            }
        }
    }

    @Test
    fun noOverlapIsReportedNotZeroError() {
        val anchor = uniformDiskRgb(200, 200, 200)
        val valid = BooleanArray(size * size) { false }
        val empty = C.CanonicalDisk(C.PixelBuffer(size, size, IntArray(size * size)), valid, size)
        val decision = CoffeePhotometric.evaluateAndGate(anchor, empty, CoffeePhotometric.Correction.IDENTITY)
        assertEquals(CoffeePhotometric.Decision.Status.NO_OVERLAP, decision.status)
        assertFalse(decision.accepted)
    }
}
