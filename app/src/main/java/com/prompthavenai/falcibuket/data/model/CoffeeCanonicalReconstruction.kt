package com.prompthavenai.falcibuket.data.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * V4 KANONİK FİNCAN-UZAYI yeniden-kurması (saf Kotlin, Android'siz).
 *
 * Kaynak fotoğraf → rim elipsi → maskeleme → kanonik disk → ÜÇ diskin TEK
 * kanonik diske füzyonu → zemin (iç bölge) + duvar (dış halka) → atlas.
 *
 * Aynı bütün-fincan görüntüsü duvara ASLA üç kez döşenmez. Tek bir fiziksel
 * nokta tek bir kanonik konuma düşer.
 *
 * Tüm örnekleme iki-doğrusal ve MASKE-FARKINDA'dır: elips dışından gelen
 * katkılar reddedilir ve kalan ağırlıklar yeniden normalize edilir.
 */
object CoffeeCanonicalReconstruction {

    const val DISK_SIZE = 512

    /** Kanonik diskte zeminin bittiği yarıçap. */
    const val FLOOR_DISK_R = 0.60f

    /** Merkez-ağırlıklı bölge ve duvara geçiş. */
    const val CENTER_DOMINANT_END = 0.45f
    const val CENTER_WALL_END = 0.90f
    const val CENTER_CORE = 0.80f
    const val CENTER_WALL = 0.34f

    /** Yan bölgelerin kanonik yönleri (tur). */
    const val SIDE_ANGLE_LEFT = 0.5f
    const val SIDE_ANGLE_RIGHT = 0.0f

    const val ALIGN_BINS = 180
    const val ALIGN_R_MIN = 0.20f
    const val ALIGN_R_MAX = 0.90f

    private const val EPS = 1e-4f

    class PixelBuffer(val width: Int, val height: Int, val argb: IntArray) {
        init {
            require(argb.size == width * height) { "argb size mismatch" }
        }
    }

    /** Kanonik disk. [valid] false olan pikseller elips dışıdır ve kullanılmaz. */
    class CanonicalDisk(
        val image: PixelBuffer,
        val valid: BooleanArray,
        val size: Int
    ) {
        init {
            require(valid.size == size * size) { "valid size mismatch" }
        }
    }

    class AtlasPixels(
        val width: Int,
        val height: Int,
        val floorRows: Int,
        val floor: IntArray,
        val wall: IntArray,
        val atlas: IntArray,
        val floorValid: Float,
        val wallValid: Float
    )

    // ---------------------------------------------------------------------
    // 1. Kanonik disk ızgarası
    // ---------------------------------------------------------------------

    fun canonicalDx(ox: Int, size: Int): Float = -1f + 2f * (ox + 0.5f) / size

    fun canonicalDy(oy: Int, size: Int): Float = 1f - 2f * (oy + 0.5f) / size

    private fun diskPixelX(dx: Float, size: Int): Float = (dx + 1f) * 0.5f * size - 0.5f

    private fun diskPixelY(dy: Float, size: Int): Float = (1f - dy) * 0.5f * size - 0.5f

    // ---------------------------------------------------------------------
    // 2. Elips → kanonik disk düzeltmesi (maske-farkında)
    // ---------------------------------------------------------------------

    fun rectify(source: PixelBuffer, geometry: CoffeeCupPhotoGeometry, size: Int = DISK_SIZE): CanonicalDisk {
        val w = source.width
        val h = source.height
        val cx = geometry.rimCenterX * w
        val cy = geometry.rimCenterY * h
        val rx = (geometry.rimRadiusX * w).coerceAtLeast(1e-3f)
        val ry = (geometry.rimRadiusY * h).coerceAtLeast(1e-3f)
        val cosr = cos(geometry.rimRotation)
        val sinr = sin(geometry.rimRotation)

        val argb = IntArray(size * size)
        val valid = BooleanArray(size * size)
        for (oy in 0 until size) {
            val dy = canonicalDy(oy, size)
            val rowBase = oy * size
            for (ox in 0 until size) {
                val dx = canonicalDx(ox, size)
                if (dx * dx + dy * dy > 1f) continue
                val ex = dx * rx
                val ey = dy * ry
                val sx = cx + cosr * ex - sinr * ey
                val sy = cy + sinr * ex + cosr * ey
                val color = sampleSourceMasked(source, cx, cy, rx, ry, cosr, sinr, sx, sy)
                if (color != null) {
                    argb[rowBase + ox] = color
                    valid[rowBase + ox] = true
                }
            }
        }
        return CanonicalDisk(PixelBuffer(size, size, argb), valid, size)
    }

    private fun insideEllipse(
        px: Float, py: Float,
        cx: Float, cy: Float,
        rx: Float, ry: Float,
        cosr: Float, sinr: Float
    ): Boolean {
        val dx = px - cx
        val dy = py - cy
        val ex = cosr * dx + sinr * dy
        val ey = -sinr * dx + cosr * dy
        val nx = ex / rx
        val ny = ey / ry
        return nx * nx + ny * ny <= 1f
    }

    private fun sampleSourceMasked(
        source: PixelBuffer,
        cx: Float, cy: Float, rx: Float, ry: Float,
        cosr: Float, sinr: Float,
        sx: Float, sy: Float
    ): Int? {
        val w = source.width
        val h = source.height
        val x0 = floor(sx).toInt()
        val y0 = floor(sy).toInt()
        val fx = sx - x0
        val fy = sy - y0
        var sr = 0f
        var sg = 0f
        var sb = 0f
        var sw = 0f
        for (ty in 0..1) {
            val py = y0 + ty
            if (py < 0 || py >= h) continue
            val wy = if (ty == 0) 1f - fy else fy
            if (wy <= 0f) continue
            for (tx in 0..1) {
                val px = x0 + tx
                if (px < 0 || px >= w) continue
                val wx = if (tx == 0) 1f - fx else fx
                val weight = wx * wy
                if (weight <= 0f) continue
                if (!insideEllipse(px + 0.5f, py + 0.5f, cx, cy, rx, ry, cosr, sinr)) continue
                val p = source.argb[py * w + px]
                sr += weight * ((p ushr 16) and 0xFF)
                sg += weight * ((p ushr 8) and 0xFF)
                sb += weight * (p and 0xFF)
                sw += weight
            }
        }
        if (sw <= EPS) return null
        return argb(
            (sr / sw + 0.5f).toInt().coerceIn(0, 255),
            (sg / sw + 0.5f).toInt().coerceIn(0, 255),
            (sb / sw + 0.5f).toInt().coerceIn(0, 255)
        )
    }

    // ---------------------------------------------------------------------
    // 3. Diski kanonik eksende döndürme (maske-farkında)
    // ---------------------------------------------------------------------

    fun rotateDisk(source: CanonicalDisk, turns: Float): CanonicalDisk {
        if (abs(turns) < 1e-6f) return source
        val size = source.size
        val a = turns * 2f * PI.toFloat()
        val ca = cos(a)
        val sa = sin(a)
        val argb = IntArray(size * size)
        val valid = BooleanArray(size * size)
        for (oy in 0 until size) {
            val dy = canonicalDy(oy, size)
            val rowBase = oy * size
            for (ox in 0 until size) {
                val dx = canonicalDx(ox, size)
                if (dx * dx + dy * dy > 1f) continue
                val sdx = ca * dx + sa * dy
                val sdy = -sa * dx + ca * dy
                val color = sampleDiskMasked(source, sdx, sdy)
                if (color != null) {
                    argb[rowBase + ox] = color
                    valid[rowBase + ox] = true
                }
            }
        }
        return CanonicalDisk(PixelBuffer(size, size, argb), valid, size)
    }

    fun sampleDiskMasked(disk: CanonicalDisk, dx: Float, dy: Float): Int? {
        if (dx * dx + dy * dy > 1f) return null
        val size = disk.size
        val px = diskPixelX(dx, size)
        val py = diskPixelY(dy, size)
        val x0 = floor(px).toInt()
        val y0 = floor(py).toInt()
        val fx = px - x0
        val fy = py - y0
        var sr = 0f
        var sg = 0f
        var sb = 0f
        var sw = 0f
        for (ty in 0..1) {
            val yy = y0 + ty
            if (yy < 0 || yy >= size) continue
            val wy = if (ty == 0) 1f - fy else fy
            if (wy <= 0f) continue
            for (tx in 0..1) {
                val xx = x0 + tx
                if (xx < 0 || xx >= size) continue
                val wx = if (tx == 0) 1f - fx else fx
                val weight = wx * wy
                if (weight <= 0f) continue
                val idx = yy * size + xx
                if (!disk.valid[idx]) continue
                val p = disk.image.argb[idx]
                sr += weight * ((p ushr 16) and 0xFF)
                sg += weight * ((p ushr 8) and 0xFF)
                sb += weight * (p and 0xFF)
                sw += weight
            }
        }
        if (sw <= EPS) return null
        return argb(
            (sr / sw + 0.5f).toInt().coerceIn(0, 255),
            (sg / sw + 0.5f).toInt().coerceIn(0, 255),
            (sb / sw + 0.5f).toInt().coerceIn(0, 255)
        )
    }

    // ---------------------------------------------------------------------
    // 4. İçerik yön hizalaması (ucuz açısal kross-korelasyon)
    // ---------------------------------------------------------------------

    fun angularProfile(disk: CanonicalDisk, bins: Int = ALIGN_BINS): FloatArray {
        val acc = FloatArray(bins)
        val count = IntArray(bins)
        val size = disk.size
        val rMinSq = ALIGN_R_MIN * ALIGN_R_MIN
        val rMaxSq = ALIGN_R_MAX * ALIGN_R_MAX
        for (oy in 0 until size) {
            val dy = canonicalDy(oy, size)
            val rowBase = oy * size
            for (ox in 0 until size) {
                val idx = rowBase + ox
                if (!disk.valid[idx]) continue
                val dx = canonicalDx(ox, size)
                val rSq = dx * dx + dy * dy
                if (rSq < rMinSq || rSq > rMaxSq) continue
                var angle = atan2f(dy, dx) / (2f * PI.toFloat())
                if (angle < 0f) angle += 1f
                val bin = (angle * bins).toInt().coerceIn(0, bins - 1)
                val p = disk.image.argb[idx]
                val lum = (0.299f * ((p ushr 16) and 0xFF) +
                    0.587f * ((p ushr 8) and 0xFF) +
                    0.114f * (p and 0xFF)) / 255f
                acc[bin] += lum
                count[bin]++
            }
        }
        val out = FloatArray(bins)
        for (i in 0 until bins) {
            out[i] = if (count[i] > 0) acc[i] / count[i] else 0f
        }
        return out
    }

    /**
     * [source]'u [anchor]'a hizalamak için uygulanacak içerik dönüşünü (tur)
     * kestirir. Güvenilir bir kazanç yoksa identity döner; asla zorla döndürmez.
     */
    fun estimateAlignment(anchor: CanonicalDisk, source: CanonicalDisk, bins: Int = ALIGN_BINS): PhotoAlignment {
        val pa = angularProfile(anchor, bins)
        val ps = angularProfile(source, bins)
        if (energy(pa) < ALIGN_MIN_ENERGY || energy(ps) < ALIGN_MIN_ENERGY) {
            return PhotoAlignment.IDENTITY
        }
        val ma = mean(pa)
        val ms = mean(ps)
        var bestShift = 0
        var bestErr = Float.MAX_VALUE
        var zeroErr = 0f
        for (s in 0 until bins) {
            var err = 0f
            for (b in 0 until bins) {
                val sb = (b + s) % bins
                err += abs((pa[b] - ma) - (ps[sb] - ms))
            }
            if (s == 0) zeroErr = err
            if (err < bestErr) {
                bestErr = err
                bestShift = s
            }
        }
        if (bestErr >= zeroErr - ALIGN_MARGIN) {
            return PhotoAlignment(0f, 0f, true)
        }
        var shift = bestShift
        if (shift > bins / 2) shift -= bins
        val turns = -shift.toFloat() / bins
        val confidence = ((zeroErr - bestErr) / max(zeroErr, 1e-3f)).coerceIn(0f, 1f)
        return PhotoAlignment(turns, confidence, true)
    }

    private const val ALIGN_MIN_ENERGY = 0.004f
    private const val ALIGN_MARGIN = 1e-4f

    private fun energy(p: FloatArray): Float {
        val m = mean(p)
        var e = 0f
        for (v in p) {
            val d = v - m
            e += d * d
        }
        return e / p.size
    }

    private fun mean(p: FloatArray): Float {
        var s = 0f
        for (v in p) s += v
        return s / p.size
    }

    // ---------------------------------------------------------------------
    // 5. Füzyon: üç gözlem → TEK kanonik disk
    // ---------------------------------------------------------------------

    fun fusionWeightsInto(radius: Float, angleTurns: Float, out: FloatArray) {
        val t = smoothstep(CENTER_DOMINANT_END, CENTER_WALL_END, radius)
        val center = CENTER_CORE + (CENTER_WALL - CENTER_CORE) * t
        val sideTotal = (1f - center).coerceAtLeast(0f)
        val prefL = EPS + sidePreference(angleTurns, SIDE_ANGLE_LEFT)
        val prefR = EPS + sidePreference(angleTurns, SIDE_ANGLE_RIGHT)
        val sumPref = prefL + prefR
        val nearSide = sideTotal * 0.5f
        val rimSideL = sideTotal * prefL / sumPref
        val rimSideR = sideTotal * prefR / sumPref
        out[0] = nearSide + (rimSideL - nearSide) * t
        out[1] = center
        out[2] = nearSide + (rimSideR - nearSide) * t
        normalizeInto(out)
    }

    private fun sidePreference(angleTurns: Float, sideAngle: Float): Float {
        val d = angleTurns - sideAngle
        val c = cos(d * 2f * PI.toFloat())
        return if (c <= 0f) 0f else c * c
    }

    class FusionResult(
        val disk: CanonicalDisk,
        val weights: FloatArray,
        val validCoverage: Float
    )

    fun fuse(
        observations: Array<out CanonicalDisk?>,
        alignments: Array<out PhotoAlignment>,
        size: Int = DISK_SIZE
    ): FusionResult {
        val argb = IntArray(size * size)
        val valid = BooleanArray(size * size)
        val weightImage = FloatArray(size * size * 3)
        val w = FloatArray(3)
        val applied = FloatArray(3)
        var validCount = 0
        var domainCount = 0
        for (oy in 0 until size) {
            val dy = canonicalDy(oy, size)
            val rowBase = oy * size
            for (ox in 0 until size) {
                val dx = canonicalDx(ox, size)
                val rSq = dx * dx + dy * dy
                if (rSq > 1f) continue
                domainCount++
                val r = kotlin.math.sqrt(rSq)
                var angle = atan2f(dy, dx) / (2f * PI.toFloat())
                if (angle < 0f) angle += 1f
                fusionWeightsInto(r, angle, w)

                var sr = 0f
                var sg = 0f
                var sb = 0f
                var sw = 0f
                applied[0] = 0f
                applied[1] = 0f
                applied[2] = 0f
                for (i in 0..2) {
                    val disk = observations[i] ?: continue
                    val rotation = alignments[i].contentRotation
                    val color = sampleDiskRotated(disk, dx, dy, rotation) ?: continue
                    val weight = w[i]
                    if (weight <= 0f) continue
                    sr += weight * linearFromSrgb((color ushr 16) and 0xFF)
                    sg += weight * linearFromSrgb((color ushr 8) and 0xFF)
                    sb += weight * linearFromSrgb(color and 0xFF)
                    sw += weight
                    applied[i] = weight
                }
                val idx = rowBase + ox
                if (sw <= EPS) continue
                for (i in 0..2) {
                    weightImage[idx * 3 + i] = applied[i] / sw
                }
                argb[idx] = argb(
                    srgbFromLinear(sr / sw),
                    srgbFromLinear(sg / sw),
                    srgbFromLinear(sb / sw)
                )
                valid[idx] = true
                validCount++
            }
        }
        val coverage = if (domainCount == 0) 0f else 100f * validCount / domainCount
        return FusionResult(
            CanonicalDisk(PixelBuffer(size, size, argb), valid, size),
            weightImage,
            coverage
        )
    }

    private fun sampleDiskRotated(disk: CanonicalDisk, dx: Float, dy: Float, rotation: Float): Int? {
        if (abs(rotation) < 1e-6f) return sampleDiskMasked(disk, dx, dy)
        val a = rotation * 2f * PI.toFloat()
        val ca = cos(a)
        val sa = sin(a)
        val sdx = ca * dx + sa * dy
        val sdy = -sa * dx + ca * dy
        return sampleDiskMasked(disk, sdx, sdy)
    }

    // ---------------------------------------------------------------------
    // 6. Atlas: zemin (iç) + duvar (halka) kutupsal örnekleme
    // ---------------------------------------------------------------------

    fun renderAtlas(
        fused: CanonicalDisk,
        width: Int,
        height: Int,
        floorV: Float,
        floorDiskR: Float = FLOOR_DISK_R
    ): AtlasPixels {
        val floorRows = (floorV * height).toInt().coerceIn(1, height - 1)
        val wallRows = height - floorRows
        val floorPixels = IntArray(width * floorRows)
        val wallPixels = IntArray(width * wallRows)
        var floorValid = 0
        var wallValid = 0
        val twoPi = 2f * PI.toFloat()
        for (y in 0 until height) {
            val isFloor = y < floorRows
            // Her bant kendi içinde tam [0,1] aralığına normalize edilir; böylece
            // zeminin son satırı ve duvarın ilk satırı tam olarak diskR=FLOOR_DISK_R'de
            // buluşur (süreksizlik yok).
            val diskR: Float
            if (isFloor) {
                val t = if (floorRows > 1) y.toFloat() / (floorRows - 1) else 0f
                diskR = floorDiskR * t
            } else {
                val tw = if (wallRows > 1) (y - floorRows).toFloat() / (wallRows - 1) else 0f
                diskR = floorDiskR + (1f - floorDiskR) * tw
            }
            val target = if (isFloor) floorPixels else wallPixels
            val targetBase = if (isFloor) y * width else (y - floorRows) * width
            for (x in 0 until width) {
                val u = (x + 0.5f) / width
                val theta = twoPi * u
                val dx = diskR * cos(theta)
                val dy = diskR * sin(theta)
                val sampled = sampleDiskMasked(fused, dx, dy)
                if (sampled != null) {
                    if (isFloor) floorValid++ else wallValid++
                }
                target[targetBase + x] = sampled ?: IVORY
            }
        }
        val atlas = IntArray(width * height)
        System.arraycopy(floorPixels, 0, atlas, 0, floorPixels.size)
        System.arraycopy(wallPixels, 0, atlas, floorPixels.size, wallPixels.size)
        return AtlasPixels(
            width, height, floorRows, floorPixels, wallPixels, atlas,
            100f * floorValid / floorPixels.size,
            100f * wallValid / wallPixels.size
        )
    }

    /**
     * DEBUG: zeminin Kartezyen (kutupsal olmayan) görünümü. Bir işaret merkezin
     * solunda ise burada da solunda kalır; yatay şeride dönüşmez.
     */
    fun renderFloorCartesian(fused: CanonicalDisk, size: Int, floorDiskR: Float = FLOOR_DISK_R): IntArray {
        val out = IntArray(size * size)
        for (oy in 0 until size) {
            val dy = floorDiskR - 2f * floorDiskR * (oy + 0.5f) / size
            val rowBase = oy * size
            for (ox in 0 until size) {
                val dx = -floorDiskR + 2f * floorDiskR * (ox + 0.5f) / size
                out[rowBase + ox] = sampleDiskMasked(fused, dx, dy) ?: IVORY
            }
        }
        return out
    }

    fun renderWeights(weights: FloatArray, size: Int): IntArray {
        val out = IntArray(size * size)
        for (oy in 0 until size) {
            val dy = canonicalDy(oy, size)
            val rowBase = oy * size
            for (ox in 0 until size) {
                val dx = canonicalDx(ox, size)
                val idx = rowBase + ox
                out[idx] = if (dx * dx + dy * dy > 1f) {
                    IVORY
                } else {
                    argb(
                        (weights[idx * 3] * 255f).toInt().coerceIn(0, 255),
                        (weights[idx * 3 + 1] * 255f).toInt().coerceIn(0, 255),
                        (weights[idx * 3 + 2] * 255f).toInt().coerceIn(0, 255)
                    )
                }
            }
        }
        return out
    }

    fun measureCoverage(atlas: AtlasPixels, fused: CanonicalDisk): Triple<Float, Float, Float> {
        var canonicalValid = 0
        var canonicalDomain = 0
        for (oy in 0 until fused.size) {
            val dy = canonicalDy(oy, fused.size)
            val rowBase = oy * fused.size
            for (ox in 0 until fused.size) {
                val dx = canonicalDx(ox, fused.size)
                if (dx * dx + dy * dy > 1f) continue
                canonicalDomain++
                if (fused.valid[rowBase + ox]) canonicalValid++
            }
        }
        val canonical = if (canonicalDomain == 0) 0f else 100f * canonicalValid / canonicalDomain
        return Triple(canonical, atlas.floorValid, atlas.wallValid)
    }

    // ---------------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------------

    const val IVORY = (0xFF shl 24) or (243 shl 16) or (238 shl 8) or 228

    fun argb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge1 <= edge0) return if (x < edge0) 0f else 1f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun normalizeInto(out: FloatArray) {
        var sum = 0f
        for (w in out) sum += w
        if (sum <= EPS) {
            for (i in out.indices) out[i] = 1f / out.size
            return
        }
        for (i in out.indices) out[i] /= sum
    }

    private fun atan2f(y: Float, x: Float): Float = kotlin.math.atan2(y.toDouble(), x.toDouble()).toFloat()

    private val SRGB_TO_LINEAR = FloatArray(256) { i ->
        val c = i / 255f
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    private const val LINEAR_LUT_SIZE = 1024
    private val LINEAR_TO_SRGB = FloatArray(LINEAR_LUT_SIZE + 1) { i ->
        val c = i.toFloat() / LINEAR_LUT_SIZE
        if (c <= 0.0031308f) 12.92f * c else 1.055f * c.pow(1f / 2.4f) - 0.055f
    }

    fun linearFromSrgb(value: Int): Float = SRGB_TO_LINEAR[value.coerceIn(0, 255)]

    fun srgbFromLinear(linear: Float): Int {
        val c = linear.coerceIn(0f, 1f)
        val scaled = c * LINEAR_LUT_SIZE
        val i0 = scaled.toInt().coerceIn(0, LINEAR_LUT_SIZE - 1)
        val f = scaled - i0
        val v = LINEAR_TO_SRGB[i0] + f * (LINEAR_TO_SRGB[i0 + 1] - LINEAR_TO_SRGB[i0])
        return (v * 255f + 0.5f).toInt().coerceIn(0, 255)
    }
}
