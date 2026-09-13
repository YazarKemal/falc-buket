package com.prompthavenai.falcibuket.data.model

/**
 * Üç bölgeli iç-yüzey fotoğrafı için yumuşak geçişli (feathered) atlas tanımı.
 * Bölgeler sert 1/3 dilimler DEĞİLDİR; her biri komşusuyla üst üste biner ve
 * 0/1 dairesel dikişi de harmanlanır. Ağırlıklar [CoffeeGroundMask] ile üretilir.
 */
object CoffeeCupAtlas {
    const val REGION_COUNT = 3

    /** Nihai atlas boyutu (opak, fildişi zemin + telve). */
    const val ATLAS_WIDTH = 1536
    const val ATLAS_HEIGHT = 512

    /** Her fotoğrafın işlendiği kare boyut. */
    const val PHOTO_SIZE = 512

    /** Üçgen harmanlama yarı-genişliği (U uzayında). >1/6 olmalı ki komşular örtüşsün. */
    const val BLEND_HALF_WIDTH = 0.32f

    /** Bölgelerin U merkezleri: LEFT 1/6, CENTER 3/6, RIGHT 5/6. */
    val REGION_CENTERS = floatArrayOf(1f / 6f, 3f / 6f, 5f / 6f)

    fun regionIndex(region: CoffeeCupRegion): Int = when (region) {
        CoffeeCupRegion.LEFT_INNER -> 0
        CoffeeCupRegion.CENTER_INNER -> 1
        CoffeeCupRegion.RIGHT_INNER -> 2
    }

    fun regionCenterU(region: CoffeeCupRegion): Float = REGION_CENTERS[regionIndex(region)]
}
