package com.prompthavenai.falcibuket.data.model

import com.prompthavenai.falcibuket.data.model.CoffeeCanonicalReconstruction as C
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Kabul-kapılı (acceptance-gated) fotometrik düzeltme.
 *
 * V3'ün hatası: düzeltme tüm örtüşme deltalarını KÖTÜLEŞTİRDİ. V4'te her
 * aday düzeltme önce/sonra ölçülür ve YALNIZCA iyileştiriyorsa kabul edilir.
 * Aksi halde identity (düzeltme yok). CENTER fotometrik çıpadır ve asla
 * sayısal olarak kötüleştirilmez.
 */
object CoffeePhotometric {

    const val PORCELAIN_MIN_LUM = 0.60f
    const val PORCELAIN_MAX_CHROMA = 0.20f
    const val MIN_SAMPLES = 32
    const val MIN_IMPROVEMENT = 1e-4f
    const val MIN_GAIN = 0.85f
    const val MAX_GAIN = 1.18f

    class Correction(
        val gain: FloatArray,
        val offset: FloatArray
    ) {
        val isIdentity: Boolean
            get() = abs(gain[0] - 1f) < 1e-5f && abs(gain[1] - 1f) < 1e-5f &&
                abs(gain[2] - 1f) < 1e-5f

        companion object {
            val IDENTITY = Correction(floatArrayOf(1f, 1f, 1f), floatArrayOf(0f, 0f, 0f))
        }
    }

    class Decision(
        val before: Float,
        val after: Float,
        val accepted: Boolean,
        val samples: Int,
        val status: Status
    ) {
        enum class Status { ACCEPTED, REJECTED, NO_OVERLAP }
    }

    fun estimateCandidate(anchor: C.CanonicalDisk, source: C.CanonicalDisk): Correction {
        val size = anchor.size
        var ar = 0.0
        var ag = 0.0
        var ab = 0.0
        var sr = 0.0
        var sg = 0.0
        var sb = 0.0
        var count = 0
        for (i in 0 until size * size) {
            if (!anchor.valid[i] || !source.valid[i]) continue
            val ap = anchor.image.argb[i]
            val sp = source.image.argb[i]
            if (!isPorcelain(ap) || !isPorcelain(sp)) continue
            ar += (ap ushr 16) and 0xFF
            ag += (ap ushr 8) and 0xFF
            ab += ap and 0xFF
            sr += (sp ushr 16) and 0xFF
            sg += (sp ushr 8) and 0xFF
            sb += sp and 0xFF
            count++
        }
        if (count < MIN_SAMPLES) return Correction.IDENTITY
        val gain = FloatArray(3)
        gain[0] = (ar / sr).toFloat().coerceIn(MIN_GAIN, MAX_GAIN)
        gain[1] = (ag / sg).toFloat().coerceIn(MIN_GAIN, MAX_GAIN)
        gain[2] = (ab / sb).toFloat().coerceIn(MIN_GAIN, MAX_GAIN)
        return Correction(gain, floatArrayOf(0f, 0f, 0f))
    }

    fun evaluateAndGate(
        anchor: C.CanonicalDisk,
        source: C.CanonicalDisk,
        candidate: Correction
    ): Decision {
        val size = anchor.size
        var before = 0.0
        var after = 0.0
        var count = 0
        for (i in 0 until size * size) {
            if (!anchor.valid[i] || !source.valid[i]) continue
            val ap = anchor.image.argb[i]
            val sp = source.image.argb[i]
            val ar = (ap ushr 16) and 0xFF
            val ag = (ap ushr 8) and 0xFF
            val ab = ap and 0xFF
            val sr = (sp ushr 16) and 0xFF
            val sg = (sp ushr 8) and 0xFF
            val sb = sp and 0xFF
            before += (abs(ar - sr) + abs(ag - sg) + abs(ab - sb)) / 3.0
            val cr = applyChannel(sr, candidate.gain[0], candidate.offset[0])
            val cg = applyChannel(sg, candidate.gain[1], candidate.offset[1])
            val cb = applyChannel(sb, candidate.gain[2], candidate.offset[2])
            after += (abs(ar - cr) + abs(ag - cg) + abs(ab - cb)) / 3.0
            count++
        }
        if (count < MIN_SAMPLES) {
            return Decision(0f, 0f, false, count, Decision.Status.NO_OVERLAP)
        }
        val beforeAvg = (before / count).toFloat() / 255f
        val afterAvg = (after / count).toFloat() / 255f
        val finite = beforeAvg.isFinite() && afterAvg.isFinite()
        val accepted = finite && candidate.gain.all { it.isFinite() } &&
            afterAvg < beforeAvg - MIN_IMPROVEMENT
        return Decision(
            beforeAvg,
            if (accepted) afterAvg else beforeAvg,
            accepted,
            count,
            if (accepted) Decision.Status.ACCEPTED else Decision.Status.REJECTED
        )
    }

    fun applyToDisk(source: C.CanonicalDisk, correction: Correction): C.CanonicalDisk {
        if (correction.isIdentity) return source
        val size = source.size
        val argb = source.image.argb.copyOf()
        val valid = source.valid.copyOf()
        for (i in 0 until size * size) {
            if (!source.valid[i]) continue
            val p = source.image.argb[i]
            argb[i] = C.argb(
                applyChannel((p ushr 16) and 0xFF, correction.gain[0], correction.offset[0]),
                applyChannel((p ushr 8) and 0xFF, correction.gain[1], correction.offset[1]),
                applyChannel(p and 0xFF, correction.gain[2], correction.offset[2])
            )
        }
        return C.CanonicalDisk(C.PixelBuffer(size, size, argb), valid, size)
    }

    private fun applyChannel(value: Int, gain: Float, offset: Float): Int {
        val linear = C.linearFromSrgb(value)
        val corrected = linear * gain + offset
        return C.srgbFromLinear(corrected)
    }

    private fun isPorcelain(p: Int): Boolean {
        val r = (p ushr 16) and 0xFF
        val g = (p ushr 8) and 0xFF
        val b = p and 0xFF
        val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        val chroma = (max(r, max(g, b)) - min(r, min(g, b))) / 255f
        return lum >= PORCELAIN_MIN_LUM && chroma <= PORCELAIN_MAX_CHROMA
    }
}
