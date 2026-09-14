package com.prompthavenai.falcibuket.data.model

import com.prompthavenai.falcibuket.data.model.CoffeeCanonicalReconstruction as C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CoffeeCanonicalHardeningTest {

    private val size = 128

    private fun uniformDisk(color: Int): C.CanonicalDisk {
        val argb = IntArray(size * size)
        val mask = BooleanArray(size * size)
        for (oy in 0 until size) {
            val dy = C.canonicalDy(oy, size)
            for (ox in 0 until size) {
                val dx = C.canonicalDx(ox, size)
                if (dx * dx + dy * dy > 1f) continue
                mask[oy * size + ox] = true
                argb[oy * size + ox] = color
            }
        }
        return C.CanonicalDisk(C.PixelBuffer(size, size, argb), mask, size)
    }

    /** Yön (dx>=0) ile değişen disk: merkez ortalamasını test etmek için. */
    private fun angleVaryingDisk(): C.CanonicalDisk {
        val argb = IntArray(size * size)
        val mask = BooleanArray(size * size)
        for (oy in 0 until size) {
            val dy = C.canonicalDy(oy, size)
            for (ox in 0 until size) {
                val dx = C.canonicalDx(ox, size)
                if (dx * dx + dy * dy > 1f) continue
                mask[oy * size + ox] = true
                argb[oy * size + ox] = if (dx >= 0f) C.argb(255, 255, 255) else C.argb(0, 0, 0)
            }
        }
        return C.CanonicalDisk(C.PixelBuffer(size, size, argb), mask, size)
    }

    // ---- renderAtlas edge dimensions --------------------------------------

    @Test
    fun renderAtlasWidthOne() {
        val atlas = C.renderAtlas(uniformDisk(C.IVORY), 1, 32, 0.28f)
        assertEquals(1, atlas.width)
        assertEquals(32, atlas.height)
        assertEquals(32, atlas.atlas.size)
    }

    @Test
    fun renderAtlasHeightOneHasEmptyWall() {
        val atlas = C.renderAtlas(uniformDisk(C.IVORY), 16, 1, 0.28f)
        assertEquals(1, atlas.floorRows)
        assertEquals(0, atlas.wall.size)
        assertEquals(16, atlas.atlas.size)
        assertEquals(0f, atlas.wallValid, 1e-3f)
        assertTrue(atlas.floorValid.isFinite())
    }

    @Test
    fun renderAtlasOneByOne() {
        val atlas = C.renderAtlas(uniformDisk(C.IVORY), 1, 1, 0.28f)
        assertEquals(1, atlas.atlas.size)
    }

    @Test
    fun renderAtlasZeroAndNegativeDimensionsRejected() {
        val disk = uniformDisk(C.IVORY)
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 0, 32, 0.28f) }
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 32, 0, 0.28f) }
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, -1, 32, 0.28f) }
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 32, -1, 0.28f) }
    }

    @Test
    fun renderAtlasInvalidParametersRejected() {
        val disk = uniformDisk(C.IVORY)
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 32, 32, 0f) }
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 32, 32, 1f) }
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 32, 32, Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 32, 32, 0.28f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { C.renderAtlas(disk, 32, 32, 0.28f, 1f) }
    }

    // ---- angular weights ---------------------------------------------------

    @Test
    fun weightsSumToOneOverDenseGrid() {
        val w = FloatArray(3)
        var r = 0f
        while (r <= 1.0001f) {
            var u = 0f
            while (u < 1f) {
                C.fusionWeightsInto(r, u, w)
                assertEquals("sum r=$r u=$u", 1f, w.sum(), 1e-4f)
                for (v in w) assertTrue("nonneg", v >= 0f)
                u += 1f / 512f
            }
            r += 0.02f
        }
    }

    @Test
    fun weightsArePeriodicAcrossWrap() {
        val a = FloatArray(3)
        val b = FloatArray(3)
        var u = 0f
        while (u < 1f) {
            C.fusionWeightsInto(0.8f, u, a)
            C.fusionWeightsInto(0.8f, u + 1f, b)
            for (i in 0..2) assertEquals("wrap i=$i u=$u", a[i], b[i], 1e-5f)
            u += 1f / 64f
        }
    }

    @Test
    fun weightsAngleIndependentThroughCore() {
        val a = FloatArray(3)
        val b = FloatArray(3)
        C.fusionWeightsInto(0.3f, 0.1f, a)
        C.fusionWeightsInto(0.3f, 0.63f, b)
        for (i in 0..2) assertEquals(a[i], b[i], 1e-5f)
        C.fusionWeightsInto(0f, 0f, a)
        assertTrue("center dominates floor", a[1] > a[0] + a[2])
    }

    @Test
    fun weightsTransitionHasBoundedDelta() {
        val w0 = FloatArray(3)
        val w1 = FloatArray(3)
        val step = 1f / 4096f
        C.fusionWeightsInto(1f, 0f, w0)
        var u = 0f
        while (u < 1f) {
            u += step
            C.fusionWeightsInto(1f, u % 1f, w1)
            for (i in 0..2) {
                assertTrue("abrupt weight jump i=$i at u=$u", abs(w1[i] - w0[i]) < 0.006f)
            }
            w0[0] = w1[0]; w0[1] = w1[1]; w0[2] = w1[2]
        }
    }

    // ---- floor center ------------------------------------------------------

    @Test
    fun floorCenterRowIsAngleIndependent() {
        val atlas = C.renderAtlas(angleVaryingDisk(), 64, 32, 0.28f)
        val first = atlas.floor[0]
        for (x in 0 until atlas.width) {
            assertEquals("center row must be angle-independent", first, atlas.floor[x])
        }
    }

    @Test
    fun angularVariationDecaysTowardCenter() {
        val atlas = C.renderAtlas(angleVaryingDisk(), 64, 32, 0.28f)
        fun variation(row: Int): Int {
            var lo = 255
            var hi = 0
            for (x in 0 until atlas.width) {
                val red = (atlas.floor[row * atlas.width + x] ushr 16) and 0xFF
                if (red < lo) lo = red
                if (red > hi) hi = red
            }
            return hi - lo
        }
        val centerVariation = variation(0)
        val outerVariation = variation(1)
        assertTrue("center variation $centerVariation must be small", centerVariation <= 2)
        assertTrue(
            "center ($centerVariation) must be smoother than r≈0.086 ($outerVariation)",
            centerVariation < outerVariation
        )
    }

    @Test
    fun offCenterMarkerRemainsLocalized() {
        val argb = IntArray(size * size)
        val mask = BooleanArray(size * size)
        val dotX = -0.3f
        val dotY = 0.1f
        for (oy in 0 until size) {
            val dy = C.canonicalDy(oy, size)
            for (ox in 0 until size) {
                val dx = C.canonicalDx(ox, size)
                if (dx * dx + dy * dy > 1f) continue
                mask[oy * size + ox] = true
                val d = kotlin.math.hypot((dx - dotX).toDouble(), (dy - dotY).toDouble())
                argb[oy * size + ox] = if (d < 0.06) C.argb(20, 20, 20) else C.IVORY
            }
        }
        val disk = C.CanonicalDisk(C.PixelBuffer(size, size, argb), mask, size)
        val cartesian = C.renderFloorCartesian(disk, 128)
        var dark = 0
        for (p in cartesian) if (((p ushr 16) and 0xFF) < 80) dark++
        assertTrue("marker visible", dark > 0)
        assertTrue("marker localized, got $dark", dark < 128 * 128 / 8)
    }

    // ---- rim feather -------------------------------------------------------

    @Test
    fun rimFeatherLeavesFloorInteriorUnchanged() {
        val dark = C.argb(20, 20, 20)
        val atlas = C.renderAtlas(uniformDisk(dark), 64, 32, 0.28f)
        for (y in 0 until atlas.floorRows) {
            for (x in 0 until atlas.width) {
                assertEquals("floor interior unchanged", dark, atlas.floor[y * atlas.width + x])
            }
        }
    }

    @Test
    fun rimFeatherBlendsOuterWallTowardIvory() {
        val dark = C.argb(20, 20, 20)
        val atlas = C.renderAtlas(uniformDisk(dark), 64, 32, 0.28f)
        val lastWallRow = atlas.height - atlas.floorRows - 1
        var lighter = 0
        for (x in 0 until atlas.width) {
            val red = (atlas.wall[lastWallRow * atlas.width + x] ushr 16) and 0xFF
            if (red > 20) lighter++
        }
        assertTrue("outer wall edge must be feathered toward ivory", lighter > 0)
    }

    @Test
    fun rimFeatherDoesNotReduceCoverage() {
        val atlas = C.renderAtlas(uniformDisk(C.IVORY), 64, 32, 0.28f)
        val coverage = C.measureCoverage(atlas, uniformDisk(C.IVORY))
        assertTrue("canonical ${coverage.first}", coverage.first >= 99f)
        assertTrue("floor ${coverage.second}", coverage.second >= 99f)
        assertTrue("wall ${coverage.third}", coverage.third >= 95f)
    }
}
