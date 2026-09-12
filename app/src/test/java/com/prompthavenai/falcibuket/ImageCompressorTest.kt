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

    @Test
    fun `scaled size keeps landscape aspect within max edge`() {
        assertEquals(1600 to 800, ImageCompressor.computeScaledSize(3200, 1600))
    }

    @Test
    fun `scaled size keeps portrait aspect within max edge`() {
        assertEquals(800 to 1600, ImageCompressor.computeScaledSize(1600, 3200))
    }

    @Test
    fun `scaled size never upscales`() {
        assertEquals(800 to 600, ImageCompressor.computeScaledSize(800, 600))
    }

    @Test
    fun `scaled size handles degenerate input`() {
        assertEquals(1 to 1, ImageCompressor.computeScaledSize(0, 0))
        assertEquals(1 to 1, ImageCompressor.computeScaledSize(100, 100, 0))
    }

    @Test
    fun `jpeg signature detection`() {
        assertTrue(ImageCompressor.isJpeg(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
        assertEquals(false, ImageCompressor.isJpeg(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte())))
        assertEquals(false, ImageCompressor.isJpeg(byteArrayOf(0xFF.toByte())))
        assertEquals(false, ImageCompressor.isJpeg(ByteArray(0)))
    }
}
