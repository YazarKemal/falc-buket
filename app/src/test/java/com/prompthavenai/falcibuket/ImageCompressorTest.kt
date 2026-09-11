package com.prompthavenai.falcibuket

import com.prompthavenai.falcibuket.data.remote.ImageCompressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCompressorTest {

    private fun isPowerOfTwo(n: Int) = n > 0 && (n and (n - 1)) == 0

    @Test
    fun `small image stays unsampled`() {
        assertEquals(1, ImageCompressor.computeSampleSize(800, 600))
    }

    @Test
    fun `decoded edge stays within 2x max edge for camera photos`() {
        val sample = ImageCompressor.computeSampleSize(4000, 3000)
        assertTrue(isPowerOfTwo(sample))
        assertTrue(4000 / sample <= ImageCompressor.MAX_EDGE * 2)
    }

    @Test
    fun `huge photo is heavily sampled`() {
        val sample = ImageCompressor.computeSampleSize(12000, 9000)
        assertTrue(sample >= 4)
        assertTrue(12000 / sample <= ImageCompressor.MAX_EDGE * 2)
    }

    @Test
    fun `degenerate dimensions return 1`() {
        assertEquals(1, ImageCompressor.computeSampleSize(0, 0))
        assertEquals(1, ImageCompressor.computeSampleSize(-5, 100))
    }

    @Test
    fun `custom max edge respected`() {
        val sample = ImageCompressor.computeSampleSize(2000, 1000, 400)
        assertTrue(2000 / sample <= 400 * 2)
    }
}
