package com.prompthavenai.falcibuket.data.model

import com.prompthavenai.falcibuket.data.model.CoffeeCanonicalReconstruction as C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class CoffeeCanonicalReconstructionTest {

    private val size = 256

    private fun buffer(w: Int, h: Int, fill: Int): C.PixelBuffer =
        C.PixelBuffer(w, h, IntArray(w * h) { fill })

    private fun ivory(): Int = C.IVORY

    private fun dark(): Int = C.argb(30, 24, 18)

    private fun magenta(): Int = C.argb(255, 0, 255)

    /** Elips dışını [background], içini [pattern] ile dolduran sentetik kaynak. */
    private fun syntheticSource(
        w: Int,
        h: Int,
        geometry: CoffeeCupPhotoGeometry,
        background: Int,
        pattern: (Float, Float) -> Int
    ): C.PixelBuffer {
        val argb = IntArray(w * h) { background }
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
                if (nx * nx + ny * ny <= 1f) {
                    argb[py * w + px] = pattern(nx, ny)
                }
            }
        }
        return C.PixelBuffer(w, h, argb)
    }

    private fun defaultGeometry(): CoffeeCupPhotoGeometry =
        CoffeeCupPhotoGeometry(0.5f, 0.5f, 0.4f, 0.4f, 0f)

    private fun rectify(source: C.PixelBuffer, geometry: CoffeeCupPhotoGeometry = defaultGeometry()): C.CanonicalDisk =
        C.rectify(source, geometry, size)

    // ---- Test B: outside-ellipse rejection ---------------------------------

    @Test
    fun outsideEllipseMagentaNeverEntersDisk() {
        val geometry = defaultGeometry()
        val source = syntheticSource(400, 400, geometry, magenta()) { _, _ -> ivory() }
        val disk = rectify(source, geometry)
        var contaminated = 0
        var valid = 0
        for (i in disk.valid.indices) {
            if (!disk.valid[i]) continue
            valid++
            val p = disk.image.argb[i]
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            if (r > 200 && g < 60 && b > 200) contaminated++
        }
        assertTrue("valid pixels expected", valid > 0)
        assertEquals("magenta contamination", 0, contaminated)
    }

    @Test
    fun insideEllipseReddishBrownIsPreserved() {
        val geometry = defaultGeometry()
        val brown = C.argb(150, 80, 50)
        val source = syntheticSource(400, 400, geometry, magenta()) { dx, dy ->
            if (dx * dx + dy * dy < 0.2f) brown else ivory()
        }
        val disk = rectify(source, geometry)
        var found = false
        for (i in disk.valid.indices) {
            if (disk.valid[i] && disk.image.argb[i] == brown) {
                found = true
                break
            }
        }
        assertTrue("interior brown must survive", found)
    }

    // ---- Test A: same object is not tiled ----------------------------------

    @Test
    fun fusedDiskContainsSingleMarkerNotThreeCopies() {
        val geometry = defaultGeometry()
        val markerR = 0.82f
        val pattern: (Float, Float) -> Int = { dx, dy ->
            if (hypot(dx.toDouble(), dy.toDouble()) > markerR - 0.08 &&
                hypot(dx.toDouble(), dy.toDouble()) < markerR + 0.08 &&
                dy > 0f
            ) dark() else ivory()
        }
        val source = syntheticSource(400, 400, geometry, magenta(), pattern)
        val disk = rectify(source, geometry)
        val observations = arrayOf(disk, disk, disk)
        val alignments = arrayOf(PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY)
        val fused = C.fuse(observations, alignments, size)
        val atlas = C.renderAtlas(fused.disk, 512, 256, 0.28f)
        val clusters = countWallMarkerClusters(atlas, 512, 80)
        assertEquals("exactly one wall marker cluster", 1, clusters)
    }

    @Test
    fun fusionOfRotatedObservationsYieldsOneMarkerNotThree() {
        val geometry = defaultGeometry()
        val blob: (Float, Float) -> Int = { dx, dy ->
            if (hypot(dx.toDouble(), (dy - 0.72f).toDouble()) < 0.22) dark() else ivory()
        }
        val source = syntheticSource(400, 400, geometry, magenta(), blob)
        val base = rectify(source, geometry)
        val rotatedLeft = C.rotateDisk(base, 0.12f)
        val rotatedRight = C.rotateDisk(base, -0.12f)
        val observations = arrayOf(base, rotatedLeft, rotatedRight)

        val alignments = arrayOf(
            PhotoAlignment.IDENTITY,
            C.estimateAlignment(base, rotatedLeft),
            C.estimateAlignment(base, rotatedRight)
        )
        val fused = C.fuse(observations, alignments, size)
        val aligned = countWallMarkerClusters(C.renderAtlas(fused.disk, 512, 256, 0.28f), 512, 200)
        assertEquals("aligned fusion must show ONE marker", 1, aligned)

        val naive = C.fuse(
            observations,
            arrayOf(PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY),
            size
        )
        val tiled = countWallMarkerClusters(C.renderAtlas(naive.disk, 512, 256, 0.28f), 512, 200)
        assertTrue("unaligned fusion must show multiple markers, got $tiled", tiled >= 2)
    }

    /** Duvar bandında koyu işaretin açısal küme (cluster) sayısı (wrap dahil). */
    private fun countWallMarkerClusters(atlas: C.AtlasPixels, width: Int, threshold: Int): Int {
        val bins = 360
        val dark = BooleanArray(bins)
        for (y in atlas.floorRows until atlas.height) {
            for (x in 0 until width) {
                val p = atlas.wall[(y - atlas.floorRows) * width + x]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                if (r < threshold && g < threshold && b < threshold) {
                    dark[(x * bins / width) % bins] = true
                }
            }
        }
        var runs = 0
        for (i in 0 until bins) {
            if (dark[i] && !dark[(i - 1 + bins) % bins]) runs++
        }
        return runs
    }

    // ---- Test C: floor spatial coherence -----------------------------------

    @Test
    fun floorDotStaysLeftOfCenterAndLocalized() {
        val geometry = defaultGeometry()
        val dotX = -0.25f
        val dotY = 0.10f
        val source = syntheticSource(400, 400, geometry, magenta()) { dx, dy ->
            if (hypot((dx - dotX).toDouble(), (dy - dotY).toDouble()) < 0.06) dark() else ivory()
        }
        val disk = rectify(source, geometry)
        val fused = C.fuse(
            arrayOf(disk, disk, disk),
            arrayOf(PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY),
            size
        )
        val cartesian = C.renderFloorCartesian(fused.disk, 256)
        var sumX = 0.0
        var sumY = 0.0
        var count = 0
        for (oy in 0 until 256) {
            for (ox in 0 until 256) {
                val p = cartesian[oy * 256 + ox]
                val r = (p ushr 16) and 0xFF
                if (r < 80) {
                    sumX += ox
                    sumY += oy
                    count++
                }
            }
        }
        assertTrue("dot must be visible", count > 0)
        val cx = sumX / count
        assertTrue("dot must stay left of center, got $cx", cx < 128)
        assertTrue("dot must stay localized, got $count px", count < 256 * 256 / 8)
    }

    @Test
    fun floorDotDoesNotBecomeHorizontalStripe() {
        val geometry = defaultGeometry()
        val source = syntheticSource(400, 400, geometry, magenta()) { dx, dy ->
            if (hypot((dx + 0.25f).toDouble(), (dy - 0.10f).toDouble()) < 0.06) dark() else ivory()
        }
        val disk = rectify(source, geometry)
        val fused = C.fuse(
            arrayOf(disk, disk, disk),
            arrayOf(PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY),
            size
        )
        val atlas = C.renderAtlas(fused.disk, 512, 256, 0.28f)
        val width = 512
        var maxDarkPerColumn = 0
        for (x in 0 until width) {
            var darkInColumn = 0
            for (y in 0 until atlas.floorRows) {
                val p = atlas.floor[y * width + x]
                if (((p ushr 16) and 0xFF) < 80) darkInColumn++
            }
            if (darkInColumn > maxDarkPerColumn) maxDarkPerColumn = darkInColumn
        }
        assertTrue("floor row must not be fully dark (stripe)", maxDarkPerColumn < atlas.floorRows)
    }

    // ---- Test F: mirroring / orientation -----------------------------------

    @Test
    fun asymmetricMarkerStaysOnExpectedSide() {
        val geometry = defaultGeometry()
        val source = syntheticSource(400, 400, geometry, magenta()) { dx, dy ->
            if (hypot((dx + 0.3f).toDouble(), dy.toDouble()) < 0.08) dark() else ivory()
        }
        val disk = rectify(source, geometry)
        val fused = C.fuse(
            arrayOf(disk, disk, disk),
            arrayOf(PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY),
            size
        )
        val cartesian = C.renderFloorCartesian(fused.disk, 256)
        var sumX = 0.0
        var count = 0
        for (oy in 0 until 256) {
            for (ox in 0 until 256) {
                val p = cartesian[oy * 256 + ox]
                if (((p ushr 16) and 0xFF) < 80) {
                    sumX += ox
                    count++
                }
            }
        }
        assertTrue(count > 0)
        assertTrue("left marker must remain left", sumX / count < 128)
    }

    // ---- Test E: canonical coverage ----------------------------------------

    @Test
    fun fullCoverageForThreeValidObservations() {
        val geometry = defaultGeometry()
        val source = syntheticSource(400, 400, geometry, magenta()) { _, _ -> ivory() }
        val disk = rectify(source, geometry)
        val fused = C.fuse(
            arrayOf(disk, disk, disk),
            arrayOf(PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY),
            size
        )
        val atlas = C.renderAtlas(fused.disk, 512, 256, 0.28f)
        val coverage = C.measureCoverage(atlas, fused.disk)
        assertTrue("canonical coverage ${coverage.first}", coverage.first >= 99f)
        assertTrue("floor coverage ${coverage.second}", coverage.second >= 99f)
        assertTrue("wall coverage ${coverage.third}", coverage.third >= 99f)
    }

    // ---- Rectification fidelity --------------------------------------------

    @Test
    fun rectificationRecoversPatternWithinTolerance() {
        val geometry = CoffeeCupPhotoGeometry(0.52f, 0.48f, 0.38f, 0.42f, 0.2f)
        val source = syntheticSource(512, 512, geometry, magenta()) { dx, dy ->
            val v = ((dx + 1f) * 0.5f * 255f).toInt().coerceIn(0, 255)
            C.argb(v, 128, 64)
        }
        val disk = C.rectify(source, geometry, 256)
        var maxError = 0
        var checked = 0
        for (oy in 0 until 256) {
            val dy = C.canonicalDy(oy, 256)
            for (ox in 0 until 256) {
                val dx = C.canonicalDx(ox, 256)
                if (dx * dx + dy * dy > 0.8f) continue
                val i = oy * 256 + ox
                if (!disk.valid[i]) continue
                val expected = ((dx + 1f) * 0.5f * 255f).toInt().coerceIn(0, 255)
                val actual = (disk.image.argb[i] ushr 16) and 0xFF
                maxError = maxOf(maxError, kotlin.math.abs(expected - actual))
                checked++
            }
        }
        assertTrue("checked enough pixels", checked > 1000)
        assertTrue("rectify red-channel error $maxError", maxError <= 6)
    }

    // ---- Fusion weights -----------------------------------------------------

    @Test
    fun fusionWeightsNormalizeAndCenterDominatesFloor() {
        val w = FloatArray(3)
        var r = 0f
        while (r <= 1f) {
            var angle = 0f
            while (angle < 1f) {
                C.fusionWeightsInto(r, angle, w)
                assertEquals("sum r=$r a=$angle", 1f, w.sum(), 1e-3f)
                for (v in w) assertTrue(v >= 0f)
                angle += 0.05f
            }
            r += 0.05f
        }
        C.fusionWeightsInto(0f, 0f, w)
        assertTrue("center dominates floor", w[1] > w[0] + w[2])
    }

    @Test
    fun fusionWeightsAreAngleIndependentAtCenter() {
        val a = FloatArray(3)
        val b = FloatArray(3)
        C.fusionWeightsInto(0f, 0f, a)
        C.fusionWeightsInto(0f, 0.37f, b)
        for (i in 0..2) assertEquals(a[i], b[i], 1e-4f)
    }

    // ---- Alignment ----------------------------------------------------------

    @Test
    fun alignmentRecoversKnownRotation() {
        val geometry = defaultGeometry()
        val source = syntheticSource(400, 400, geometry, magenta()) { dx, dy ->
            val angle = atan2(dy.toDouble(), dx.toDouble())
            if (angle > -0.35 && angle < 0.35 && hypot(dx.toDouble(), dy.toDouble()) > 0.3) dark() else ivory()
        }
        val anchor = rectify(source, geometry)
        val rotated = C.rotateDisk(anchor, 0.12f)
        val alignment = C.estimateAlignment(anchor, rotated)
        assertEquals("recovered rotation", -0.12f, alignment.contentRotation, 0.03f)
    }

    @Test
    fun alignmentIdentityForUnrotatedDisk() {
        val geometry = defaultGeometry()
        val source = syntheticSource(400, 400, geometry, magenta()) { dx, dy ->
            val angle = atan2(dy.toDouble(), dx.toDouble())
            if (angle > -0.35 && angle < 0.35 && hypot(dx.toDouble(), dy.toDouble()) > 0.3) dark() else ivory()
        }
        val anchor = rectify(source, geometry)
        val alignment = C.estimateAlignment(anchor, anchor)
        assertEquals(0f, alignment.contentRotation, 1e-4f)
    }

    // ---- Atlas continuity ---------------------------------------------------

    @Test
    fun atlasFloorAndWallAreContinuousAtBoundary() {
        val geometry = defaultGeometry()
        // Radyal gradyan: yarıçap süreksizliği renkte görünür olur.
        val source = syntheticSource(400, 400, geometry, magenta()) { dx, dy ->
            val v = (hypot(dx.toDouble(), dy.toDouble()) * 255.0).toInt().coerceIn(0, 255)
            C.argb(v, v, v)
        }
        val disk = rectify(source, geometry)
        val fused = C.fuse(
            arrayOf(disk, disk, disk),
            arrayOf(PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY, PhotoAlignment.IDENTITY),
            size
        )
        val atlas = C.renderAtlas(fused.disk, 256, 128, 0.28f)
        val width = 256
        val lastFloorRow = atlas.floorRows - 1
        for (x in 0 until width) {
            val floorPixel = atlas.floor[lastFloorRow * width + x]
            val wallPixel = atlas.wall[x]
            val fr = (floorPixel ushr 16) and 0xFF
            val wr = (wallPixel ushr 16) and 0xFF
            assertTrue("floor/wall radial jump at x=$x (floor=$fr wall=$wr)", kotlin.math.abs(fr - wr) <= 4)
        }
    }
}
