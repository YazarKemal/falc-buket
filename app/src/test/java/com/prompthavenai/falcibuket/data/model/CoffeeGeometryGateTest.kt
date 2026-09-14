package com.prompthavenai.falcibuket.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoffeeGeometryGateTest {

    private fun geometry() = CoffeeCupPhotoGeometry(0.5f, 0.5f, 0.3f, 0.3f, 0f)

    private fun invalidGeometry() = CoffeeCupPhotoGeometry(0.5f, 0.5f, 0.01f, 0.3f, 0f)

    @Test
    fun thresholdIsInclusive() {
        val decision = CoffeeGeometryGate.select(
            null,
            RimEstimate(geometry(), CoffeeRimEstimator.CONFIDENCE_THRESHOLD, RimEstimate.Status.OK)
        )
        assertNotNull(decision.geometry)
        assertFalse(decision.needsCalibration)
    }

    @Test
    fun belowThresholdRejectsValidLookingGeometry() {
        val decision = CoffeeGeometryGate.select(
            null,
            RimEstimate(
                geometry(),
                CoffeeRimEstimator.CONFIDENCE_THRESHOLD - 0.01f,
                RimEstimate.Status.LOW_CONFIDENCE
            )
        )
        assertNull(decision.geometry)
        assertTrue(decision.needsCalibration)
    }

    @Test
    fun lowConfidenceStatusIsRejectedEvenWithHighConfidence() {
        val decision = CoffeeGeometryGate.select(
            null,
            RimEstimate(geometry(), 0.99f, RimEstimate.Status.LOW_CONFIDENCE)
        )
        assertNull(decision.geometry)
        assertTrue(decision.needsCalibration)
    }

    @Test
    fun nonFiniteConfidenceIsRejected() {
        val decision = CoffeeGeometryGate.select(
            null,
            RimEstimate(geometry(), Float.NaN, RimEstimate.Status.OK)
        )
        assertNull(decision.geometry)
        assertTrue(decision.needsCalibration)
    }

    @Test
    fun missingOrInvalidGeometryRequiresCalibration() {
        assertTrue(CoffeeGeometryGate.select(null, null).needsCalibration)
        assertTrue(
            CoffeeGeometryGate.select(
                null,
                RimEstimate(null, 1f, RimEstimate.Status.LOW_CONFIDENCE)
            ).needsCalibration
        )
        assertTrue(
            CoffeeGeometryGate.select(
                null,
                RimEstimate(invalidGeometry(), 1f, RimEstimate.Status.OK)
            ).needsCalibration
        )
    }

    @Test
    fun manualGeometryOverridesLowConfidence() {
        val decision = CoffeeGeometryGate.select(
            geometry(),
            RimEstimate(null, 0f, RimEstimate.Status.LOW_CONFIDENCE)
        )
        assertNotNull(decision.geometry)
        assertFalse(decision.needsCalibration)
    }

    @Test
    fun invalidManualFallsBackToReliableAuto() {
        val decision = CoffeeGeometryGate.select(
            invalidGeometry(),
            RimEstimate(geometry(), 1f, RimEstimate.Status.OK)
        )
        assertNotNull(decision.geometry)
        assertFalse(decision.needsCalibration)
    }
}
