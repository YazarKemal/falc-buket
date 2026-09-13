package com.prompthavenai.falcibuket.data.model

import com.prompthavenai.falcibuket.data.model.CoffeeCanonicalReconstruction as C
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hafif, ağırlıksız (CV framework'süz) rim elipsi kestiricisi.
 *
 * Küçültülmüş gri görüntü üzerinde kısıtlı bir aday taraması yapar:
 * merkez görüntü merkezine yakın, genişlik ~%45–95, yükseklik ~%30–95,
 * sınırlı en-boy oranı ve dönüş. Adayları çevresel gradyan şiddeti ve
 * kenar sürekliliği ile puanlar.
 *
 * Güven düşükse tüm-görüntü varsayımı YAPILMAZ; çağıran manuel kalibrasyon ister.
 */
object CoffeeRimEstimator {

    const val CONFIDENCE_THRESHOLD = 0.35f
    private const val DOWNSCALE = 160
    private const val ANGLE_SAMPLES = 48
    private const val EDGE_STRONG = 0.06f
    private const val MIN_SCORE = 0.015f
    private const val GOOD_SCORE = 0.10f

    private class Gray(val data: FloatArray, val width: Int, val height: Int)

    fun estimate(source: C.PixelBuffer): RimEstimate {
        if (source.width < 16 || source.height < 16) {
            return RimEstimate(null, 0f, RimEstimate.Status.INVALID_INPUT)
        }
        val gray = downscaleLuminance(source)
        var best: CoffeeCupPhotoGeometry? = null
        var bestScore = -1f

        var cx = 0.40f
        while (cx <= 0.601f) {
            var cy = 0.35f
            while (cy <= 0.651f) {
                var rx = 0.225f
                while (rx <= 0.476f) {
                    var ry = 0.15f
                    while (ry <= 0.476f) {
                        var rot = -0.5236f
                        while (rot <= 0.5237f) {
                            val score = score(gray, cx, cy, rx, ry, rot)
                            if (score > bestScore) {
                                bestScore = score
                                best = CoffeeCupPhotoGeometry(cx, cy, rx, ry, rot)
                            }
                            rot += 0.5236f
                        }
                        ry += 0.05f
                    }
                    rx += 0.05f
                }
                cy += 0.05f
            }
            cx += 0.05f
        }

        if (best == null || bestScore < 0f) {
            return RimEstimate(null, 0f, RimEstimate.Status.LOW_CONFIDENCE)
        }
        val refined = refine(gray, best)
        val refinedScore = score(gray, refined.rimCenterX, refined.rimCenterY, refined.rimRadiusX, refined.rimRadiusY, refined.rimRotation)
        val geometry = if (refinedScore >= bestScore) refined else best
        val finalScore = maxOf(refinedScore, bestScore)
        val confidence = ((finalScore - MIN_SCORE) / (GOOD_SCORE - MIN_SCORE)).coerceIn(0f, 1f)
        val status = if (confidence >= CONFIDENCE_THRESHOLD) {
            RimEstimate.Status.OK
        } else {
            RimEstimate.Status.LOW_CONFIDENCE
        }
        return RimEstimate(geometry, confidence, status)
    }

    private fun refine(gray: Gray, start: CoffeeCupPhotoGeometry): CoffeeCupPhotoGeometry {
        var cx = start.rimCenterX
        var cy = start.rimCenterY
        var rx = start.rimRadiusX
        var ry = start.rimRadiusY
        var rot = start.rimRotation
        var step = 0.025f
        repeat(3) {
            var bestScore = score(gray, cx, cy, rx, ry, rot)
            var bestCx = cx
            var bestCy = cy
            var bestRx = rx
            var bestRy = ry
            var bestRot = rot
            for (dcx in -1..1) {
                for (dcy in -1..1) {
                    for (drx in -1..1) {
                        for (dry in -1..1) {
                            for (drot in -1..1) {
                                val ncx = (cx + dcx * step).coerceIn(0.30f, 0.70f)
                                val ncy = (cy + dcy * step).coerceIn(0.25f, 0.75f)
                                val nrx = (rx + drx * step).coerceIn(0.15f, 0.50f)
                                val nry = (ry + dry * step).coerceIn(0.10f, 0.50f)
                                val nrot = (rot + drot * step).coerceIn(-0.7f, 0.7f)
                                val s = score(gray, ncx, ncy, nrx, nry, nrot)
                                if (s > bestScore) {
                                    bestScore = s
                                    bestCx = ncx
                                    bestCy = ncy
                                    bestRx = nrx
                                    bestRy = nry
                                    bestRot = nrot
                                }
                            }
                        }
                    }
                }
            }
            cx = bestCx
            cy = bestCy
            rx = bestRx
            ry = bestRy
            rot = bestRot
            step *= 0.5f
        }
        return CoffeeCupPhotoGeometry(cx, cy, rx, ry, rot)
    }

    private fun score(gray: Gray, cx: Float, cy: Float, rx: Float, ry: Float, rot: Float): Float {
        val cosr = cos(rot)
        val sinr = sin(rot)
        var sum = 0f
        var strong = 0
        var outOfBounds = 0
        for (k in 0 until ANGLE_SAMPLES) {
            val t = 2f * PI.toFloat() * k / ANGLE_SAMPLES
            val ex = rx * cos(t)
            val ey = ry * sin(t)
            val px = cx + cosr * ex - sinr * ey
            val py = cy + sinr * ex + cosr * ey
            val vx = px - cx
            val vy = py - cy
            val innerX = cx + vx * 0.90f
            val innerY = cy + vy * 0.90f
            val outerX = cx + vx * 1.08f
            val outerY = cy + vy * 1.08f
            if (outerX < 0f || outerX > 1f || outerY < 0f || outerY > 1f) {
                outOfBounds++
                continue
            }
            val inner = sampleGray(gray, innerX, innerY)
            val outer = sampleGray(gray, outerX, outerY)
            val diff = abs(inner - outer)
            sum += diff
            if (diff > EDGE_STRONG) strong++
        }
        val valid = ANGLE_SAMPLES - outOfBounds
        if (valid < ANGLE_SAMPLES / 2) return -1f
        val meanDiff = sum / valid
        val continuity = strong.toFloat() / valid
        val boundsPenalty = 1f - outOfBounds.toFloat() / ANGLE_SAMPLES
        return meanDiff * continuity * boundsPenalty
    }

    private fun sampleGray(gray: Gray, nx: Float, ny: Float): Float {
        val x = (nx * (gray.width - 1)).coerceIn(0f, (gray.width - 1).toFloat())
        val y = (ny * (gray.height - 1)).coerceIn(0f, (gray.height - 1).toFloat())
        val x0 = x.toInt().coerceIn(0, gray.width - 1)
        val y0 = y.toInt().coerceIn(0, gray.height - 1)
        val x1 = (x0 + 1).coerceAtMost(gray.width - 1)
        val y1 = (y0 + 1).coerceAtMost(gray.height - 1)
        val fx = x - x0
        val fy = y - y0
        val v00 = gray.data[y0 * gray.width + x0]
        val v10 = gray.data[y0 * gray.width + x1]
        val v01 = gray.data[y1 * gray.width + x0]
        val v11 = gray.data[y1 * gray.width + x1]
        return v00 * (1 - fx) * (1 - fy) + v10 * fx * (1 - fy) +
            v01 * (1 - fx) * fy + v11 * fx * fy
    }

    private fun downscaleLuminance(source: C.PixelBuffer): Gray {
        val longest = maxOf(source.width, source.height)
        val scale = if (longest <= DOWNSCALE) 1 else (longest.toFloat() / DOWNSCALE).toInt().coerceAtLeast(1)
        val w = (source.width / scale).coerceAtLeast(8)
        val h = (source.height / scale).coerceAtLeast(8)
        val out = FloatArray(w * h)
        for (oy in 0 until h) {
            for (ox in 0 until w) {
                var sr = 0f
                var sg = 0f
                var sb = 0f
                var n = 0
                for (dy in 0 until scale) {
                    val py = oy * scale + dy
                    if (py >= source.height) break
                    for (dx in 0 until scale) {
                        val px = ox * scale + dx
                        if (px >= source.width) break
                        val p = source.argb[py * source.width + px]
                        sr += (p ushr 16) and 0xFF
                        sg += (p ushr 8) and 0xFF
                        sb += p and 0xFF
                        n++
                    }
                }
                if (n == 0) n = 1
                out[oy * w + ox] = (0.299f * sr + 0.587f * sg + 0.114f * sb) / (255f * n)
            }
        }
        return Gray(out, w, h)
    }
}
