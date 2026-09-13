package com.prompthavenai.falcibuket.data.model

import com.prompthavenai.falcibuket.data.model.CoffeeCanonicalReconstruction as C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class CoffeeRimEstimatorTest {

    private fun ellipseImage(
        w: Int,
        h: Int,
        geometry: CoffeeCupPhotoGeometry,
        inside: Int,
        outside: Int
    ): C.PixelBuffer {
        val argb = IntArray(w * h) { outside }
        val cx = geometry.rimCenterX * w
        val cy = geometry.rimCenterY * h
        val rx = geometry.rimRadiusX * w
        val ry = geometry.rimRadiusY * h
        val cosr = cos(geometry.rimRotation)
        val sinr = sin(geometry.rimRotation)
        for (py in 0 until h) {
            for (px in 0 until w) {
                val dxs = px + 0.5f - cx
                val dys = py + 0.5f - cy
                val ex = cosr * dxs + sinr * dys
                val ey = -sinr * dxs + cosr * dys
                val nx = ex / rx
                val ny = ey / ry
                if (nx * nx + ny * ny <= 1f) argb[py * w + px] = inside
            }
        }
        return C.PixelBuffer(w, h, argb)
    }

    @Test
    fun detectsStrongCircularRim() {
        val geometry = CoffeeCupPhotoGeometry(0.5f, 0.5f, 0.3f, 0.3f, 0f)
        val image = ellipseImage(300, 300, geometry, C.argb(220, 215, 205), C.argb(25, 25, 25))
        val estimate = CoffeeRimEstimator.estimate(image)
        assertNotNull(estimate.geometry)
        assertEquals(RimEstimate.Status.OK, estimate.status)
        val g = estimate.geometry!!
        assertTrue("cx ${g.rimCenterX}", abs(g.rimCenterX - 0.5f) < 0.08f)
        assertTrue("cy ${g.rimCenterY}", abs(g.rimCenterY - 0.5f) < 0.08f)
        assertTrue("rx ${g.rimRadiusX}", abs(g.rimRadiusX - 0.3f) < 0.07f)
        assertTrue("ry ${g.rimRadiusY}", abs(g.rimRadiusY - 0.3f) < 0.07f)
    }

    @Test
    fun uniformImageIsLowConfidence() {
        val image = C.PixelBuffer(200, 200, IntArray(200 * 200) { C.argb(128, 128, 128) })
        val estimate = CoffeeRimEstimator.estimate(image)
        assertEquals(RimEstimate.Status.LOW_CONFIDENCE, estimate.status)
    }

    @Test
    fun tinyImageIsInvalidInput() {
        val image = C.PixelBuffer(8, 8, IntArray(64) { C.argb(10, 10, 10) })
        val estimate = CoffeeRimEstimator.estimate(image)
        assertEquals(RimEstimate.Status.INVALID_INPUT, estimate.status)
    }
}
