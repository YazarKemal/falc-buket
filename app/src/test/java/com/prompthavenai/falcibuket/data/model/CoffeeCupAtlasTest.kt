package com.prompthavenai.falcibuket.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoffeeCupAtlasTest {

    @Test
    fun regionCentersAreOrderedLeftCenterRight() {
        val left = CoffeeCupAtlas.regionCenterU(CoffeeCupRegion.LEFT_INNER)
        val center = CoffeeCupAtlas.regionCenterU(CoffeeCupRegion.CENTER_INNER)
        val right = CoffeeCupAtlas.regionCenterU(CoffeeCupRegion.RIGHT_INNER)
        assertTrue(left < center)
        assertTrue(center < right)
    }

    @Test
    fun adjacentRegionsOverlapIncludingWrap() {
        val halfWidth = CoffeeCupAtlas.BLEND_HALF_WIDTH
        val left = CoffeeCupAtlas.regionCenterU(CoffeeCupRegion.LEFT_INNER)
        val center = CoffeeCupAtlas.regionCenterU(CoffeeCupRegion.CENTER_INNER)
        val right = CoffeeCupAtlas.regionCenterU(CoffeeCupRegion.RIGHT_INNER)
        // Each pair overlaps because distance < sum of half-widths.
        assertTrue(CoffeeGroundMask.circularDistance(left, center) < 2f * halfWidth)
        assertTrue(CoffeeGroundMask.circularDistance(center, right) < 2f * halfWidth)
        assertTrue(CoffeeGroundMask.circularDistance(right, left) < 2f * halfWidth)
    }

    @Test
    fun atlasDimensionsArePositiveAndStripLike() {
        assertTrue(CoffeeCupAtlas.ATLAS_WIDTH > CoffeeCupAtlas.ATLAS_HEIGHT)
        assertTrue(CoffeeCupAtlas.ATLAS_HEIGHT >= 256)
        assertEquals(3, CoffeeCupAtlas.REGION_COUNT)
    }
}
