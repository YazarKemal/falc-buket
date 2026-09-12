package com.prompthavenai.falcibuket.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementBadgeTest {

    @Test
    fun badgeOrderAndLabelsMatchSpec() {
        assertEquals(
            listOf(
                "İlk Fal",
                "Tarot Yolcusu",
                "Aşkın İzinde",
                "Günlük Bağ",
                "Buket Seni Tanıyor",
                "Buket+"
            ),
            achievementBadges.map { it.label }
        )
    }

    @Test
    fun exactlyThreeBadgesAreUnlocked() {
        assertEquals(3, achievementBadges.count { it.unlocked })
        assertEquals(
            listOf("İlk Fal", "Tarot Yolcusu", "Günlük Bağ"),
            achievementBadges.filter { it.unlocked }.map { it.label }
        )
        assertEquals(
            listOf("Aşkın İzinde", "Buket Seni Tanıyor", "Buket+"),
            achievementBadges.filterNot { it.unlocked }.map { it.label }
        )
    }

    @Test
    fun columnsAdaptToAvailableWidth() {
        assertEquals(2, adaptiveColumns(320.dp, 104.dp, 12.dp, 6))
        assertEquals(3, adaptiveColumns(360.dp, 104.dp, 12.dp, 6))
        assertEquals(3, adaptiveColumns(411.dp, 104.dp, 12.dp, 6))
        assertEquals(6, adaptiveColumns(800.dp, 104.dp, 12.dp, 6))
        assertEquals(1, adaptiveColumns(0.dp, 104.dp, 12.dp, 6))
    }

    @Test
    fun categoryColumnsAdaptToAvailableWidth() {
        assertEquals(2, adaptiveColumns(360.dp, 160.dp, 16.dp, 4))
        assertEquals(4, adaptiveColumns(800.dp, 160.dp, 16.dp, 4))
        assertTrue(adaptiveColumns(800.dp, 160.dp, 16.dp, 4) <= 4)
    }
}
