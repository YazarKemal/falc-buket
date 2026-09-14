package com.prompthavenai.falcibuket.data.model

/**
 * Rim geometrisi seçimi (release-güvenli).
 *
 * Kural: geçerli manuel kalibrasyon daima kazanır. Aksi halde otomatik geometri
 * YALNIZCA yeterince güvenilirse kabul edilir; düşük güvende geometri null kalır
 * ve çağıran manuel kalibrasyon ister. Böylece kötü otomatik geometriyle sessizce
 * atlas kurulmaz.
 */
object CoffeeGeometryGate {

    class Decision(
        val geometry: CoffeeCupPhotoGeometry?,
        val needsCalibration: Boolean
    )

    fun select(manual: CoffeeCupPhotoGeometry?, estimate: RimEstimate?): Decision {
        val manualValid = manual?.takeIf { it.isValid() }
        if (manualValid != null) return Decision(manualValid, false)

        val auto = estimate ?: return Decision(null, true)
        val geometry = auto.geometry
        val accepted = geometry != null && geometry.isValid() &&
            auto.status == RimEstimate.Status.OK &&
            auto.confidence.isFinite() &&
            auto.confidence >= CoffeeRimEstimator.CONFIDENCE_THRESHOLD
        return if (accepted) Decision(geometry, false) else Decision(null, true)
    }
}
