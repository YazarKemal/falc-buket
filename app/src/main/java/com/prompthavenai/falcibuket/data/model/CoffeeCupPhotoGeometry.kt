package com.prompthavenai.falcibuket.data.model

import kotlin.math.abs

/**
 * Bir kaynak fotoğraftaki fincan İÇ ağzının (rim) elips geometrisi.
 *
 * Koordinat sözleşmesi (Android'siz, saf veri):
 * - [rimCenterX]/[rimCenterY]: görüntü genişliği/yüksekliğine göre normalize [0,1].
 * - [rimRadiusX]/[rimRadiusY]: yarı-eksenler; genişlik/yüksekliğe göre normalize.
 * - [rimRotation]: radyan, görüntü koordinatında saat yönünde.
 *
 * Bu elips fincan iç boşluğunu temsil eder. Elips DIŞINDA kalan pikseller
 * yeniden-kurmaya ASLA girmez (masa, arka plan, parmak, pembe/kırmızı nesneler).
 */
data class CoffeeCupPhotoGeometry(
    val rimCenterX: Float = 0.5f,
    val rimCenterY: Float = 0.5f,
    val rimRadiusX: Float = 0.35f,
    val rimRadiusY: Float = 0.35f,
    val rimRotation: Float = 0f
) {

    fun isValid(): Boolean {
        if (!rimCenterX.isFinite() || !rimCenterY.isFinite()) return false
        if (!rimRadiusX.isFinite() || !rimRadiusY.isFinite() || !rimRotation.isFinite()) return false
        if (rimRadiusX < MIN_RADIUS || rimRadiusY < MIN_RADIUS) return false
        if (rimRadiusX > MAX_RADIUS || rimRadiusY > MAX_RADIUS) return false
        if (rimCenterX < -0.25f || rimCenterX > 1.25f) return false
        if (rimCenterY < -0.25f || rimCenterY > 1.25f) return false
        return abs(rimRotation) <= MAX_ROTATION
    }

    companion object {
        const val MIN_RADIUS = 0.05f
        const val MAX_RADIUS = 0.60f
        const val MAX_ROTATION = 0.9f
    }
}

/**
 * Otomatik rim tespitinin sonucu. [confidence] düşükse tüm-görüntü varsayımı
 * YAPILMAZ; manuel kalibrasyon istenir.
 */
data class RimEstimate(
    val geometry: CoffeeCupPhotoGeometry?,
    val confidence: Float,
    val status: Status
) {
    enum class Status { OK, LOW_CONFIDENCE, INVALID_INPUT }
}

/**
 * Bir kaynak fotoğrafın içeriğinin kanonik yönelime göre döndürülmesi.
 * [contentRotation] kanonik eksende saat yönünün tersine tur cinsindendir.
 */
data class PhotoAlignment(
    val contentRotation: Float,
    val confidence: Float,
    val accepted: Boolean
) {
    companion object {
        val IDENTITY = PhotoAlignment(0f, 0f, true)
    }
}
