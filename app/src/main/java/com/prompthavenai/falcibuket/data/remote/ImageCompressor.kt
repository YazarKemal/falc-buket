package com.prompthavenai.falcibuket.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface

object ImageCompressor {
    const val MAX_EDGE = 1600
    const val JPEG_QUALITY = 84
    const val MAX_PAYLOAD_BYTES = 2_500_000L

    fun computeSampleSize(srcWidth: Int, srcHeight: Int, maxEdge: Int = MAX_EDGE): Int {
        val longest = maxOf(srcWidth, srcHeight)
        if (longest <= 0) return 1
        var sample = 1
        // Decode en fazla ~2x hedef kenarda kalır; keskin ölçekleme compress aşamasında yapılır.
        while (longest / sample > maxEdge * 2) sample *= 2
        return sample
    }

    fun compressToJpeg(
        context: Context,
        uri: Uri,
        maxEdge: Int = MAX_EDGE,
        quality: Int = JPEG_QUALITY
    ): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: throw IllegalStateException("IMAGE_INVALID: stream unavailable")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("IMAGE_INVALID: not a decodable image")
        }

        val sample = computeSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IllegalStateException("IMAGE_INVALID: decode failed")

        val rotated = applyExifRotation(context, uri, bitmap)
        val scaled = scaleToMaxEdge(rotated, maxEdge)

        val out = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val bytes = out.toByteArray()
        if (bytes.size > MAX_PAYLOAD_BYTES) {
            throw IllegalStateException("IMAGE_TOO_LARGE: ${bytes.size} bytes")
        }
        return bytes
    }

    fun toBase64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
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

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
