package com.prompthavenai.falcibuket.data.local

import com.prompthavenai.falcibuket.data.model.CoffeeCupPhotoGeometry
import com.prompthavenai.falcibuket.data.model.CoffeeCupRegion
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CoffeeSessionFilesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun files() = CoffeeSessionFiles(tmp.root)

    private fun photoFile(region: CoffeeCupRegion): File =
        File(tmp.root, CoffeeSessionFiles.fileName(region))

    private fun writePhoto(region: CoffeeCupRegion, bytes: ByteArray = byteArrayOf(1, 2, 3, 4)) {
        photoFile(region).writeBytes(bytes)
    }

    private fun geometry() = CoffeeCupPhotoGeometry(0.4f, 0.45f, 0.3f, 0.32f, 0.1f)

    @Test
    fun jsonRoundTripRestoresRevisionAndGeometry() {
        val saved = files().savePhotoBytes(CoffeeCupRegion.LEFT_INNER, byteArrayOf(9, 8, 7))
        val revision = saved.photos[CoffeeCupRegion.LEFT_INNER]!!.revision
        files().saveGeometry(CoffeeCupRegion.LEFT_INNER, geometry())

        val reloaded = CoffeeSessionFiles(tmp.root).load()
        assertEquals(revision, reloaded.photos[CoffeeCupRegion.LEFT_INNER]!!.revision)
        assertEquals(geometry(), reloaded.geometryFor(CoffeeCupRegion.LEFT_INNER))
    }

    @Test
    fun threePhotoRestore() {
        for (region in CoffeeCupRegion.entries) writePhoto(region)
        val snapshot = files().load()
        assertEquals(3, snapshot.presentCount)
        assertTrue(snapshot.isPresent(CoffeeCupRegion.LEFT_INNER))
        assertTrue(snapshot.isPresent(CoffeeCupRegion.CENTER_INNER))
        assertTrue(snapshot.isPresent(CoffeeCupRegion.RIGHT_INNER))
    }

    @Test
    fun geometryRestoreReadsRegionsObject() {
        writePhoto(CoffeeCupRegion.CENTER_INNER)
        val meta = JSONObject()
        val regions = JSONObject()
        val entry = JSONObject().apply {
            put("revision", "rev-1")
            put("cx", 0.4)
            put("cy", 0.45)
            put("rx", 0.3)
            put("ry", 0.32)
            put("rot", 0.1)
        }
        regions.put(CoffeeCupRegion.CENTER_INNER.name, entry)
        meta.put("regions", regions)
        File(tmp.root, CoffeeSessionFiles.META_NAME).writeText(meta.toString())

        val snapshot = files().load()
        assertEquals("rev-1", snapshot.photos[CoffeeCupRegion.CENTER_INNER]!!.revision)
        val geo = snapshot.geometryFor(CoffeeCupRegion.CENTER_INNER)!!
        assertEquals(0.4f, geo.rimCenterX, 1e-4f)
        assertEquals(0.45f, geo.rimCenterY, 1e-4f)
    }

    @Test
    fun malformedJsonRecoversPhotosWithDerivedRevisions() {
        writePhoto(CoffeeCupRegion.RIGHT_INNER)
        File(tmp.root, CoffeeSessionFiles.META_NAME).writeText("{ this is not json")
        val snapshot = files().load()
        assertTrue(snapshot.isPresent(CoffeeCupRegion.RIGHT_INNER))
        val file = photoFile(CoffeeCupRegion.RIGHT_INNER)
        assertEquals("${file.length()}-${file.lastModified()}", snapshot.photos[CoffeeCupRegion.RIGHT_INNER]!!.revision)
        assertNull(snapshot.geometryFor(CoffeeCupRegion.RIGHT_INNER))
    }

    @Test
    fun truncatedJsonDoesNotThrow() {
        writePhoto(CoffeeCupRegion.LEFT_INNER)
        File(tmp.root, CoffeeSessionFiles.META_NAME).writeText("{\"regions\":{\"LEFT_INNER\":{\"cx\":0.4")
        val snapshot = files().load()
        assertTrue(snapshot.isPresent(CoffeeCupRegion.LEFT_INNER))
    }

    @Test
    fun orphanTmpFilesAreDeletedWithoutTouchingFinals() {
        writePhoto(CoffeeCupRegion.LEFT_INNER)
        val orphanPhoto = File(tmp.root, "left.jpg${CoffeeSessionFiles.TMP_SUFFIX}").apply { writeBytes(byteArrayOf(1)) }
        val orphanMeta = File(tmp.root, "${CoffeeSessionFiles.META_NAME}${CoffeeSessionFiles.TMP_SUFFIX}").apply { writeText("{}") }

        val snapshot = files().load()

        assertFalse(orphanPhoto.exists())
        assertFalse(orphanMeta.exists())
        assertTrue(snapshot.isPresent(CoffeeCupRegion.LEFT_INNER))
    }

    @Test
    fun badRegionDoesNotDropOtherRegions() {
        writePhoto(CoffeeCupRegion.LEFT_INNER)
        writePhoto(CoffeeCupRegion.CENTER_INNER)
        val meta = JSONObject()
        val regions = JSONObject()
        regions.put("LEFT_INNER", "not-an-object")
        regions.put(
            "CENTER_INNER",
            JSONObject().apply {
                put("revision", "center-rev")
                put("cx", 0.5); put("cy", 0.5); put("rx", 0.3); put("ry", 0.3); put("rot", 0.0)
            }
        )
        meta.put("regions", regions)
        File(tmp.root, CoffeeSessionFiles.META_NAME).writeText(meta.toString())

        val snapshot = files().load()
        assertTrue(snapshot.isPresent(CoffeeCupRegion.LEFT_INNER))
        assertEquals("center-rev", snapshot.photos[CoffeeCupRegion.CENTER_INNER]!!.revision)
    }

    @Test
    fun invalidGeometryIsIgnored() {
        writePhoto(CoffeeCupRegion.LEFT_INNER)
        val meta = JSONObject()
        val regions = JSONObject()
        regions.put(
            "LEFT_INNER",
            JSONObject().apply {
                put("cx", 0.5); put("cy", 0.5); put("rx", 0.01); put("ry", 0.3); put("rot", 0.0)
            }
        )
        meta.put("regions", regions)
        File(tmp.root, CoffeeSessionFiles.META_NAME).writeText(meta.toString())
        assertNull(files().load().geometryFor(CoffeeCupRegion.LEFT_INNER))
    }

    @Test
    fun replacementClearsOnlyItsOwnGeometry() {
        files().savePhotoBytes(CoffeeCupRegion.LEFT_INNER, byteArrayOf(1))
        files().savePhotoBytes(CoffeeCupRegion.RIGHT_INNER, byteArrayOf(2))
        files().saveGeometry(CoffeeCupRegion.LEFT_INNER, geometry())
        files().saveGeometry(CoffeeCupRegion.RIGHT_INNER, geometry())

        val after = files().savePhotoBytes(CoffeeCupRegion.LEFT_INNER, byteArrayOf(3))

        assertNull(after.geometryFor(CoffeeCupRegion.LEFT_INNER))
        assertEquals(geometry(), after.geometryFor(CoffeeCupRegion.RIGHT_INNER))
    }

    @Test
    fun saveGeometryPreservesPhotoRevision() {
        val saved = files().savePhotoBytes(CoffeeCupRegion.LEFT_INNER, byteArrayOf(1, 2, 3))
        val revision = saved.photos[CoffeeCupRegion.LEFT_INNER]!!.revision
        val after = files().saveGeometry(CoffeeCupRegion.LEFT_INNER, geometry())
        assertEquals(revision, after.photos[CoffeeCupRegion.LEFT_INNER]!!.revision)
    }

    @Test
    fun staleGeometryRevisionIsRejected() {
        files().savePhotoBytes(CoffeeCupRegion.LEFT_INNER, byteArrayOf(1))
        val after = files().saveGeometry(CoffeeCupRegion.LEFT_INNER, geometry(), expectedPhotoRevision = "wrong-rev")
        assertNull(after.geometryFor(CoffeeCupRegion.LEFT_INNER))
    }

    @Test
    fun zeroLengthAndMissingPhotosAreSkipped() {
        photoFile(CoffeeCupRegion.LEFT_INNER).writeBytes(ByteArray(0))
        val snapshot = files().load()
        assertEquals(0, snapshot.presentCount)
    }

    @Test
    fun clearRemovesSession() {
        files().savePhotoBytes(CoffeeCupRegion.LEFT_INNER, byteArrayOf(1))
        files().clear()
        assertFalse(photoFile(CoffeeCupRegion.LEFT_INNER).exists())
        assertEquals(0, CoffeeSessionFiles(tmp.root).load().presentCount)
    }
}
