package com.prompthavenai.falcibuket.data.model

/**
 * Telve (kahve tortusu) çıkarımı ve kesintisiz atlas harmanlama için saf,
 * deterministik yardımcılar. Android/AI/ağ bağımlılığı yoktur.
 *
 * Amaç: kaynak fotoğraftaki koyu/orta kahve telve yapılarını korurken,
 * porselenin parlak/beyaz zeminini bastırmak ve üç bölgeyi sert dikdörtgen
 * sınırlar olmadan, üst üste bindirerek harmanlamak.
 */
object CoffeeGroundMask {

    // Tespit eşikleri (0..1).
    const val WHITE_LUM = 0.90f      // bu parlaklık ve üstü: porselen
    const val DARK_LUM = 0.50f       // bu parlaklık ve altı: tam telve
    const val CHROMA_FULL = 0.14f    // doygunluk kapısı
    const val IVORY_R = 243
    const val IVORY_G = 238
    const val IVORY_B = 228

    /** 0..1 arası telve alfa değeri. Beyaz/porselen → ~0, koyu telve → ~1. */
    fun groundAlpha(r: Int, g: Int, b: Int): Float {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val lum = 0.299f * rf + 0.587f * gf + 0.114f * bf
        val maxC = maxOf(rf, gf, bf)
        val minC = minOf(rf, gf, bf)
        val chroma = maxC - minC
        val lumAlpha = ((WHITE_LUM - lum) / (WHITE_LUM - DARK_LUM)).coerceIn(0f, 1f)
        val chromaGate = (chroma / CHROMA_FULL).coerceIn(0f, 1f)
        return (lumAlpha * chromaGate).coerceIn(0f, 1f)
    }

    /** Dairesel (wrap) U mesafesi. */
    fun circularDistance(a: Float, b: Float): Float {
        val d = kotlin.math.abs(a - b) % 1f
        return minOf(d, 1f - d)
    }

    /** Üçgen ağırlık: merkeze yakın 1, yarı-genişlik dışında 0. */
    fun blendWeight(u: Float, center: Float, halfWidth: Float): Float {
        if (halfWidth <= 0f) return 0f
        return (1f - circularDistance(u, center) / halfWidth).coerceIn(0f, 1f)
    }

    /** Dikey kenar yumuşatma: üst ve alt uçlarda 0'a iner. */
    fun verticalFeather(v: Float, edge: Float = 0.10f): Float {
        if (edge <= 0f) return 1f
        val bottom = (v / edge).coerceIn(0f, 1f)
        val top = ((1f - v) / edge).coerceIn(0f, 1f)
        return minOf(bottom, top)
    }

    /**
     * Verilen merkezler için normalize edilmiş üçgen ağırlıklar. Tüm ağırlıklar
     * sıfırsa en yakın merkeze 1 atanır (tam kapsama; boş bölge kalmaz).
     */
    fun blendWeights(u: Float, centers: FloatArray, halfWidth: Float): FloatArray {
        val weights = FloatArray(centers.size)
        var sum = 0f
        for (i in centers.indices) {
            weights[i] = blendWeight(u, centers[i], halfWidth)
            sum += weights[i]
        }
        if (sum < 1e-4f) {
            var best = 0
            var bestDistance = Float.MAX_VALUE
            for (i in centers.indices) {
                val d = circularDistance(u, centers[i])
                if (d < bestDistance) {
                    bestDistance = d
                    best = i
                }
            }
            weights.fill(0f)
            weights[best] = 1f
            return weights
        }
        for (i in weights.indices) weights[i] /= sum
        return weights
    }

    fun ivoryArgb(): Int = (0xFF shl 24) or (IVORY_R shl 16) or (IVORY_G shl 8) or IVORY_B

    /** Telve rengini fildişi zemin üzerine alfa ile harmanlar (opak sonuç). */
    fun compositeOverIvory(r: Int, g: Int, b: Int, alpha: Float): Int {
        val a = alpha.coerceIn(0f, 1f)
        val outR = (r * a + IVORY_R * (1f - a)).toInt().coerceIn(0, 255)
        val outG = (g * a + IVORY_G * (1f - a)).toInt().coerceIn(0, 255)
        val outB = (b * a + IVORY_B * (1f - a)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
    }
}
