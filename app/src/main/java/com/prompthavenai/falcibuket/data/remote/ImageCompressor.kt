package com.prompthavenai.falcibuket.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/** Doğrulanmış, yüklemeye hazır JPEG çıktısı. */
data class ProcessedImage(
    val jpeg: ByteArray,
    val mime: String,
    val width: Int,
    val height: Int
) {
    val byteCount: Int get() = jpeg.size
}

object ImageCompressor {
    const val MAX_EDGE = 1600
    const val JPEG_QUALITY = 84
    const val MAX_PAYLOAD_BYTES = 2_500_000L
    const val OUTPUT_MIME = "image/jpeg"

    /** Uzun kenarı hedefin en fazla ~2 katına indirecek örnekleme katsayısı. */
    fun computeSampleSize(srcWidth: Int, srcHeight: Int, maxEdge: Int = MAX_EDGE): Int {
        if (maxEdge <= 0) return 1
        val longest = maxOf(srcWidth, srcHeight)
        if (longest <= 0) return 1
        var sample = 1
        while (longest / sample > maxEdge * 2) sample *= 2
        return sample
    }

    /** En-boy oranını koruyarak, uzun kenarı [maxEdge]'i aşmayacak hedef boyut. */
    fun computeScaledSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Pair<Int, Int> {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1 to 1
        val longest = maxOf(width, height)
        if (longest <= maxEdge) return width to height
        val ratio = maxEdge.toFloat() / longest
        val w = (width * ratio).toInt().coerceAtLeast(1)
        val h = (height * ratio).toInt().coerceAtLeast(1)
        return w to h
    }

    fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 2 &&
            (bytes[0].toInt() and 0xFF) == 0xFF &&
            (bytes[1].toInt() and 0xFF) == 0xD8

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    /**
     * content:// veya file:// Uri'yi güvenli biçimde JPEG'e dönüştürür.
     * Her aşama kendi hatasını sınıflandırır; EXIF okunamazsa döndürme
     * uygulanmaz (ölümcül değil). Stream'ler her geçişte yeniden açılır.
     */
    fun process(
        context: Context,
        uri: Uri,
        maxEdge: Int = MAX_EDGE,
        quality: Int = JPEG_QUALITY
    ): ProcessedImage {
        val bounds = readBounds(context, uri)
        val sample = computeSampleSize(bounds.first, bounds.second, maxEdge)
        val decoded = decode(context, uri, sample)
        if (decoded.width <= 0 || decoded.height <= 0) {
            decoded.recycle()
            throw ImageProcessingException(FortuneErrorCode.IMAGE_DECODE_FAILED, "DECODE")
        }

        // Decode sonrası tüm aşamalar (EXIF/rotate/scale/compress) OOM dahil sınıflandırılır.
        var current: Bitmap = decoded
        try {
            val rotated = rotateIfNeeded(current, readOrientation(context, uri))
            if (rotated !== current) {
                current.recycle()
                current = rotated
            }
            val (targetW, targetH) = computeScaledSize(current.width, current.height, maxEdge)
            if (targetW != current.width || targetH != current.height) {
                val scaledBmp = Bitmap.createScaledBitmap(current, targetW, targetH, true)
                if (scaledBmp !== current) {
                    current.recycle()
                    current = scaledBmp
                }
            }

            val width = current.width
            val height = current.height
            val out = ByteArrayOutputStream()
            val compressedOk = try {
                current.compress(Bitmap.CompressFormat.JPEG, quality, out)
            } finally {
                current.recycle()
            }
            if (!compressedOk) {
                throw ImageProcessingException(FortuneErrorCode.IMAGE_PROCESSING_FAILED, "COMPRESS")
            }
            val bytes = out.toByteArray()
            if (bytes.isEmpty() || !isJpeg(bytes)) {
                throw ImageProcessingException(FortuneErrorCode.IMAGE_PROCESSING_FAILED, "COMPRESS")
            }
            if (bytes.size > MAX_PAYLOAD_BYTES) {
                throw ImageProcessingException(FortuneErrorCode.IMAGE_TOO_LARGE, "SIZE_CHECK")
            }
            return ProcessedImage(bytes, OUTPUT_MIME, width, height)
        } catch (e: ImageProcessingException) {
            recycleQuietly(current)
            throw e
        } catch (e: OutOfMemoryError) {
            recycleQuietly(current)
            throw ImageProcessingException(FortuneErrorCode.IMAGE_TOO_LARGE, "PROCESS", cause = e)
        } catch (e: Exception) {
            recycleQuietly(current)
            throw ImageProcessingException(FortuneErrorCode.IMAGE_PROCESSING_FAILED, "PROCESS", cause = e)
        }
    }

    private fun recycleQuietly(bitmap: Bitmap) {
        runCatching { if (!bitmap.isRecycled) bitmap.recycle() }
    }

    /** Sadece boyutları okur; decode sonucu null olsa bile geçerli stream başarıdır. */
    private fun readBounds(context: Context, uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) {
                    throw ImageProcessingException(FortuneErrorCode.IMAGE_UNREADABLE, "STREAM_OPEN")
                }
                // inJustDecodeBounds modunda decodeStream her zaman null döner; bu bir hata değildir.
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: ImageProcessingException) {
            throw e
        } catch (e: SecurityException) {
            throw ImageProcessingException(FortuneErrorCode.IMAGE_PERMISSION_DENIED, "STREAM_OPEN", cause = e)
        } catch (e: Exception) {
            throw ImageProcessingException(FortuneErrorCode.IMAGE_UNREADABLE, "STREAM_OPEN", cause = e)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw ImageProcessingException(FortuneErrorCode.IMAGE_DECODE_FAILED, "BOUNDS")
        }
        return options.outWidth to options.outHeight
    }

    private fun decode(context: Context, uri: Uri, sample: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap: Bitmap? = try {
            context.contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) {
                    throw ImageProcessingException(FortuneErrorCode.IMAGE_UNREADABLE, "STREAM_OPEN")
                }
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: ImageProcessingException) {
            throw e
        } catch (e: SecurityException) {
            throw ImageProcessingException(FortuneErrorCode.IMAGE_PERMISSION_DENIED, "DECODE", cause = e)
        } catch (e: OutOfMemoryError) {
            throw ImageProcessingException(FortuneErrorCode.IMAGE_TOO_LARGE, "DECODE", cause = e)
        } catch (e: Exception) {
            throw ImageProcessingException(FortuneErrorCode.IMAGE_DECODE_FAILED, "DECODE", cause = e)
        }
        return bitmap ?: throw ImageProcessingException(FortuneErrorCode.IMAGE_DECODE_FAILED, "DECODE")
    }

    /** EXIF okunamazsa normal kabul edilir; decode'u etkilemez. */
    private fun readOrientation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
