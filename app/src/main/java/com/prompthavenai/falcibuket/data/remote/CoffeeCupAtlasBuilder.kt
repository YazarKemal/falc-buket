package com.prompthavenai.falcibuket.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.prompthavenai.falcibuket.data.model.CoffeeCupAtlas
import com.prompthavenai.falcibuket.data.model.CoffeeGroundMask
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion

/**
 * Üç iç-yüzey fotoğrafını TEK ve sürekli bir telve yüzeyine dönüştürür.
 *
 * - Sert 1/3 dilimler yok; bölgeler üst üste binerek harmanlanır (0/1 dikişi dahil).
 * - Porselenin beyaz zemini bastırılır; telve alfa maskesiyle fildişi zemine
 *   harmanlanır (opak sonuç → derinlik/occlusion doğru kalır).
 * - Üst/alt uçlarda dikey feather ile kenar görünmez.
 * - Tamamen yerel; hiçbir görsel ağa gönderilmez.
 */
object CoffeeCupAtlasBuilder {

    fun build(context: Context, regionUris: Map<CoffeeCupRegion, Uri?>): Bitmap? {
        val present = CoffeeCupRegion.entries.mapNotNull { region ->
            val uri = regionUris[region] ?: return@mapNotNull null
            val pixels = loadSquarePixels(context, uri, CoffeeCupAtlas.PHOTO_SIZE) ?: return@mapNotNull null
            PresentRegion(
                centerU = CoffeeCupAtlas.regionCenterU(region),
                pixels = pixels,
                alpha = blur3x3(computeAlphaMap(pixels), CoffeeCupAtlas.PHOTO_SIZE)
            )
        }
        if (present.isEmpty()) return null
        return try {
            render(present, CoffeeCupAtlas.ATLAS_WIDTH, CoffeeCupAtlas.ATLAS_HEIGHT, CoffeeCupAtlas.PHOTO_SIZE)
        } catch (e: OutOfMemoryError) {
            render(present, CoffeeCupAtlas.ATLAS_WIDTH / 2, CoffeeCupAtlas.ATLAS_HEIGHT / 2, CoffeeCupAtlas.PHOTO_SIZE)
        } catch (e: Exception) {
            null
        }
    }

    private data class PresentRegion(val centerU: Float, val pixels: IntArray, val alpha: FloatArray)

    private fun render(
        regions: List<PresentRegion>,
        width: Int,
        height: Int,
        photoSize: Int
    ): Bitmap {
        val out = IntArray(width * height)
        val centers = FloatArray(regions.size) { regions[it].centerU }
        val halfWidth = CoffeeCupAtlas.BLEND_HALF_WIDTH
        val invW = 1f / (width - 1).coerceAtLeast(1)
        val invH = 1f / (height - 1).coerceAtLeast(1)
        val photoMax = (photoSize - 1).toFloat()

        var index = 0
        for (y in 0 until height) {
            val v = y * invH
            val vf = CoffeeGroundMask.verticalFeather(v)
            val py = (v * photoMax).toInt().coerceIn(0, photoSize - 1)
            for (x in 0 until width) {
                val u = x * invW
                val weights = CoffeeGroundMask.blendWeights(u, centers, halfWidth)
                var coffeeR = 0f
                var coffeeG = 0f
                var coffeeB = 0f
                var alpha = 0f
                for (i in regions.indices) {
                    val w = weights[i]
                    if (w <= 0f) continue
                    val region = regions[i]
                    val localU = (0.5f + (u - region.centerU) / (2f * halfWidth)).coerceIn(0f, 1f)
                    val px = (localU * photoMax).toInt().coerceIn(0, photoSize - 1)
                    val sampleIndex = py * photoSize + px
                    val sample = region.pixels[sampleIndex]
                    coffeeR += w * ((sample shr 16) and 0xFF)
                    coffeeG += w * ((sample shr 8) and 0xFF)
                    coffeeB += w * (sample and 0xFF)
                    alpha += w * region.alpha[sampleIndex]
                }
                out[index++] = CoffeeGroundMask.compositeOverIvory(
                    coffeeR.toInt(),
                    coffeeG.toInt(),
                    coffeeB.toInt(),
                    alpha * vf
                )
            }
        }
        return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun computeAlphaMap(pixels: IntArray): FloatArray {
        val alpha = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            alpha[i] = CoffeeGroundMask.groundAlpha((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF)
        }
        return alpha
    }

    private fun blur3x3(source: FloatArray, size: Int): FloatArray {
        val out = FloatArray(source.size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                var sum = 0f
                var count = 0
                for (dy in -1..1) {
                    val yy = y + dy
                    if (yy !in 0 until size) continue
                    for (dx in -1..1) {
                        val xx = x + dx
                        if (xx !in 0 until size) continue
                        sum += source[yy * size + xx]
                        count++
                    }
                }
                out[y * size + x] = sum / count
            }
        }
        return out
    }

    private fun loadSquarePixels(context: Context, uri: Uri, size: Int): IntArray? = try {
        val processed = ImageCompressor.process(context, uri, maxEdge = size)
        val decoded = BitmapFactory.decodeByteArray(processed.jpeg, 0, processed.jpeg.size)
        if (decoded == null) {
            null
        } else {
            val side = minOf(decoded.width, decoded.height)
            val left = (decoded.width - side) / 2
            val top = (decoded.height - side) / 2
            val cropped = Bitmap.createBitmap(decoded, left, top, side, side)
            val scaled = if (side != size) Bitmap.createScaledBitmap(cropped, size, size, true) else cropped
            val pixels = IntArray(size * size)
            scaled.getPixels(pixels, 0, size, 0, 0, size, size)
            if (scaled !== decoded) scaled.recycle()
            if (cropped !== decoded) cropped.recycle()
            decoded.recycle()
            pixels
        }
    } catch (e: Exception) {
        null
    }
}
