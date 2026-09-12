package com.prompthavenai.falcibuket.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Gerçek content:// URI + ContentResolver yolunu egzersiz eder (FileProvider).
 * Mock ContentResolver KULLANILMAZ; hata üreten asıl sınır test edilir.
 * Bu testler hiçbir Firebase/AI çağrısı yapmaz.
 */
@RunWith(AndroidJUnit4::class)
class ImageCompressorInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun contentUri(bytes: ByteArray, name: String): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, name)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun jpegBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.RED)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.BLUE)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    @Test
    fun contentUriJpegProcessesToJpegWithinMaxDimensions() {
        val uri = contentUri(jpegBytes(2400, 1200), "cup_${System.nanoTime()}.jpg")
        val result = ImageCompressor.process(context, uri)
        assertEquals(ImageCompressor.OUTPUT_MIME, result.mime)
        assertTrue(ImageCompressor.isJpeg(result.jpeg))
        assertTrue(result.byteCount > 0)
        assertTrue(maxOf(result.width, result.height) <= ImageCompressor.MAX_EDGE)
    }

    @Test
    fun pngIsNormalizedToJpeg() {
        val uri = contentUri(pngBytes(800, 600), "saucer_${System.nanoTime()}.png")
        val result = ImageCompressor.process(context, uri)
        assertEquals(ImageCompressor.OUTPUT_MIME, result.mime)
        assertTrue(ImageCompressor.isJpeg(result.jpeg))
    }

    @Test
    fun landscapeIsScaledToMaxEdge() {
        val uri = contentUri(jpegBytes(4000, 3000), "big_${System.nanoTime()}.jpg")
        val result = ImageCompressor.process(context, uri)
        assertEquals(ImageCompressor.MAX_EDGE, maxOf(result.width, result.height))
    }

    @Test
    fun unavailableStreamIsClassified() {
        val uri = Uri.parse(
            "content://${context.packageName}.fileprovider/camera/missing_${System.nanoTime()}.jpg"
        )
        val ex = assertThrows(ImageProcessingException::class.java) {
            ImageCompressor.process(context, uri)
        }
        assertTrue(
            ex.code == FortuneErrorCode.IMAGE_UNREADABLE || ex.code == FortuneErrorCode.IMAGE_DECODE_FAILED
        )
    }

    @Test
    fun invalidImageIsClassifiedAsDecodeFailure() {
        val uri = contentUri("bu bir görsel değil".toByteArray(), "bad_${System.nanoTime()}.jpg")
        val ex = assertThrows(ImageProcessingException::class.java) {
            ImageCompressor.process(context, uri)
        }
        assertEquals(FortuneErrorCode.IMAGE_DECODE_FAILED, ex.code)
    }

    @Test
    fun preprocessingIsLocalAndNeedsNoBackend() {
        // ImageCompressor'ın Firebase/AI bağımlılığı yoktur; çevrimdışı başarı bunu doğrular.
        val uri = contentUri(jpegBytes(1200, 1600), "local_${System.nanoTime()}.jpg")
        val result = ImageCompressor.process(context, uri)
        assertTrue(result.byteCount > 0)
        assertTrue(ImageCompressor.isJpeg(result.jpeg))
    }
}
