package com.prompthavenai.falcibuket.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoffeeCupAtlasTest {

    @Test
    fun regionIndicesAreOrderedLeftCenterRight() {
        assertEquals(0, CoffeeCupAtlas.regionIndex(CoffeeCupRegion.LEFT_INNER))
        assertEquals(1, CoffeeCupAtlas.regionIndex(CoffeeCupRegion.CENTER_INNER))
        assertEquals(2, CoffeeCupAtlas.regionIndex(CoffeeCupRegion.RIGHT_INNER))
    }

    @Test
    fun atlasDimensionsArePositiveAndStripLike() {
        assertTrue(CoffeeCupAtlas.ATLAS_WIDTH > CoffeeCupAtlas.ATLAS_HEIGHT)
        assertTrue(CoffeeCupAtlas.ATLAS_HEIGHT >= 256)
        assertEquals(3, CoffeeCupAtlas.REGION_COUNT)
    }

    @Test
    fun floorBandIsAUsableFractionOfHeight() {
        assertTrue(CoffeeCupAtlas.FLOOR_V > 0.1f)
        assertTrue(CoffeeCupAtlas.FLOOR_V < 0.5f)
    }

    @Test
    fun floorBandMatchesCanonicalFloorSplit() {
        assertTrue(CoffeeCupAtlas.FLOOR_V in 0.2f..0.35f)
        assertTrue(CoffeeCanonicalReconstruction.FLOOR_DISK_R in 0.5f..0.7f)
    }
}
