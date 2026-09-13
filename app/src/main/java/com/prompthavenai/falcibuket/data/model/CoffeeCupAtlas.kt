package com.prompthavenai.falcibuket.data.model

/**
 * Tek parça, sürekli iç-yüzey atlası tanımı (V4 kanonik fincan-uzayı).
 *
 * Doku dikeyde iki mantıksal bölgeye ayrılır ve mesh UV'siyle uyumludur:
 * - v in [0, FLOOR_V): zemin — kanonik diskin iç bölgesinin kutupsal örneklemesi.
 * - v in [FLOOR_V, 1]: duvar — kanonik diskin dış halkasının kutupsal açılımı.
 */
object CoffeeCupAtlas {
    const val REGION_COUNT = 3

    const val ATLAS_WIDTH = 2048
    const val ATLAS_HEIGHT = 1024

    /** Kaynak fotoğrafların kalıcılaştırıldığı en büyük kenar. */
    const val PHOTO_MAX_EDGE = 768

    /** Duvar/zemin ayrımı (mesh UV V). coffee_cup_inner.glb bu banda göre yeniden UV'lendi. */
    const val FLOOR_V = 0.28f

    fun regionIndex(region: CoffeeCupRegion): Int = when (region) {
        CoffeeCupRegion.LEFT_INNER -> 0
        CoffeeCupRegion.CENTER_INNER -> 1
        CoffeeCupRegion.RIGHT_INNER -> 2
    }
}
