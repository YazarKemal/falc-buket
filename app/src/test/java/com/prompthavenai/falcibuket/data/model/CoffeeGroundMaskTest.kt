package com.prompthavenai.falcibuket.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CoffeeGroundMaskTest {

    @Test
    fun porcelainAndWhiteAreSuppressed() {
        assertTrue(CoffeeGroundMask.groundAlpha(245, 240, 232) < 0.05f)
        assertTrue(CoffeeGroundMask.groundAlpha(255, 255, 255) < 0.05f)
    }

    @Test
    fun darkCoffeeGroundsArePreserved() {
        assertTrue(CoffeeGroundMask.groundAlpha(55, 38, 25) > 0.8f)
        assertTrue(CoffeeGroundMask.groundAlpha(90, 65, 40) > 0.7f)
    }

    @Test
    fun lightBrownTracesArePreservedAsMediumAlpha() {
        val alpha = CoffeeGroundMask.groundAlpha(185, 150, 120)
        assertTrue("light brown should survive", alpha in 0.2f..0.9f)
    }

    @Test
    fun alphaStaysWithinUnitBounds() {
        for (r in intArrayOf(0, 40, 128, 200, 255)) {
            for (g in intArrayOf(0, 60, 140, 210, 255)) {
                for (b in intArrayOf(0, 30, 120, 190, 255)) {
                    val a = CoffeeGroundMask.groundAlpha(r, g, b)
                    assertTrue(a in 0f..1f)
                }
            }
        }
    }

    @Test
    fun blendWeightsAreNormalizedEverywhere() {
        for (i in 0..200) {
            val weights = CoffeeGroundMask.blendWeights(
                i / 200f,
                CoffeeCupAtlas.REGION_CENTERS,
                CoffeeCupAtlas.BLEND_HALF_WIDTH
            )
            assertEquals(1f, weights.sum(), 0.001f)
        }
    }

    @Test
    fun blendWeightsChangeSmoothlyNoHardBoundary() {
        val centers = CoffeeCupAtlas.REGION_CENTERS
        val halfWidth = CoffeeCupAtlas.BLEND_HALF_WIDTH
        var previous = CoffeeGroundMask.blendWeights(0f, centers, halfWidth)
        for (i in 1..400) {
            val current = CoffeeGroundMask.blendWeights(i / 400f, centers, halfWidth)
            for (k in current.indices) {
                assertTrue("weight jump at u=${i / 400f}", abs(current[k] - previous[k]) < 0.05f)
            }
            previous = current
        }
    }

    @Test
    fun uvWrapIsContinuousAtZeroAndOne() {
        val centers = CoffeeCupAtlas.REGION_CENTERS
        val halfWidth = CoffeeCupAtlas.BLEND_HALF_WIDTH
        val atZero = CoffeeGroundMask.blendWeights(0f, centers, halfWidth)
        val atOne = CoffeeGroundMask.blendWeights(1f, centers, halfWidth)
        for (i in atZero.indices) {
            assertEquals(atZero[i], atOne[i], 0.001f)
        }
    }

    @Test
    fun verticalFeatherIsZeroAtEndsAndOneInMiddle() {
        assertEquals(0f, CoffeeGroundMask.verticalFeather(0f), 0.001f)
        assertEquals(0f, CoffeeGroundMask.verticalFeather(1f), 0.001f)
        assertEquals(1f, CoffeeGroundMask.verticalFeather(0.5f), 0.001f)
    }

    @Test
    fun compositeWithoutGroundsYieldsIvory() {
        val ivory = CoffeeGroundMask.compositeOverIvory(20, 10, 5, 0f)
        assertEquals(CoffeeGroundMask.IVORY_R, (ivory shr 16) and 0xFF)
        assertEquals(CoffeeGroundMask.IVORY_G, (ivory shr 8) and 0xFF)
        assertEquals(CoffeeGroundMask.IVORY_B, ivory and 0xFF)
    }

    @Test
    fun compositeWithFullGroundsYieldsGroundColor() {
        val result = CoffeeGroundMask.compositeOverIvory(80, 50, 30, 1f)
        assertEquals(80, (result shr 16) and 0xFF)
        assertEquals(50, (result shr 8) and 0xFF)
        assertEquals(30, result and 0xFF)
    }
}
