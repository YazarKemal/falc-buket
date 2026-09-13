package com.prompthavenai.falcibuket.data.model

/**
 * Kahve fincanı iç yüzeyinin üç bölgesi. Bir sonraki milestone'da kullanıcının
 * gerçek telve fotoğrafları bu bölgelere eşlenecek. Şimdilik yalnızca domain
 * sözleşmesidir; hiçbir görsel işleme/üretim yapılmaz.
 */
enum class CoffeeCupRegion {
    LEFT_INNER,
    CENTER_INNER,
    RIGHT_INNER
}

/** Bir iç bölgeye karşılık gelen (henüz yüklenmemiş) fotoğraf tanımı. */
data class CoffeeCupRegionImage(
    val region: CoffeeCupRegion,
    val localUri: String? = null,
    val mimeType: String? = null
)

/**
 * Gelecekte üç iç-yüzey fotoğrafını 3D fincan iç yüzeyine eşleyecek sınır.
 * BU MİLESTONE'DA UYGULANMAMIŞTIR; yalnızca mimari sınır olarak tanımlanır.
 */
interface CoffeeCupTextureMapper {
    /** Fotoğrafları iç yüzeye eşler. Bugün desteklenmiyor. */
    fun map(images: List<CoffeeCupRegionImage>): Boolean
}
