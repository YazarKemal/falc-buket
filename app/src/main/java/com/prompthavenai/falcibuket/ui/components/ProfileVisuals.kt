package com.prompthavenai.falcibuket.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.NightBg
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextCream
import com.prompthavenai.falcibuket.ui.theme.TextMuted

/** Single source of truth for the mock/local streak value until real data lands. */
const val MOCK_STREAK_DAYS = 2

/** Profile identity header with avatar, title, compact stats and decorative artwork. */
@Composable
fun ProfileIdentityHeader(
    factCount: Int,
    streakDays: Int = MOCK_STREAK_DAYS,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SurfacePlum)
    ) {
        Image(
            painterResource(R.drawable.profile_header_mystic_identity),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(240.dp)
                .clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop,
            alpha = 0.9f
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            SurfacePlum,
                            SurfacePlum.copy(alpha = 0.92f),
                            SurfacePlum.copy(alpha = 0.30f)
                        )
                    )
                )
        )
        Column(Modifier.padding(20.dp)) {
            Image(
                painterResource(R.drawable.teller_avatar),
                contentDescription = "Avatar",
                modifier = Modifier.size(64.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "FalcıBuket Kullanıcısı",
                style = MaterialTheme.typography.titleLarge,
                color = TextCream
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderStat("$factCount", "Hatırlanan Detay")
                HeaderStat("$streakDays", "Günlük Seri")
            }
        }
    }
}

@Composable
private fun HeaderStat(value: String, label: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NightBg.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Gold)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}

/** Premium-looking daily streak card. Value is local/mock for now. */
@Composable
fun StreakCard(days: Int = MOCK_STREAK_DAYS, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfacePlum)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.streak_reward_mystic_flame),
            contentDescription = null,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Günlük Bağ", style = MaterialTheme.typography.titleMedium, color = Gold)
            Spacer(Modifier.height(4.dp))
            Text("$days günlük seri", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Text(
            "$days",
            style = MaterialTheme.typography.headlineMedium,
            color = Gold
        )
    }
}

data class AchievementBadge(
    val label: String,
    @DrawableRes val imageRes: Int,
    val unlocked: Boolean
)

val achievementBadges = listOf(
    AchievementBadge("İlk Fal", R.drawable.badge_first_reading, unlocked = true),
    AchievementBadge("Tarot Yolcusu", R.drawable.badge_tarot_master, unlocked = true),
    AchievementBadge("Aşkın İzinde", R.drawable.badge_love_seeker, unlocked = false),
    AchievementBadge("Günlük Bağ", R.drawable.badge_streak_7, unlocked = true),
    AchievementBadge("Buket Seni Tanıyor", R.drawable.badge_memory_bond, unlocked = false),
    AchievementBadge("Buket+", R.drawable.badge_premium_member, unlocked = false)
)

/** Adaptive achievement grid. Phone: 2-3 columns, tablet: up to 6. */
@Composable
fun AchievementSection(
    badges: List<AchievementBadge> = achievementBadges,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text("Buket Yolculuğun", style = MaterialTheme.typography.titleLarge, color = Gold)
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = adaptiveColumns(
                availableWidth = maxWidth,
                minCellWidth = 104.dp,
                spacing = 12.dp,
                max = 6
            )
            SimpleGrid(
                items = badges,
                columns = columns,
                horizontalSpacing = 12.dp,
                verticalSpacing = 16.dp
            ) { badge ->
                BadgeItem(badge)
            }
        }
    }
}

@Composable
private fun BadgeItem(badge: AchievementBadge) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(88.dp)) {
            Image(
                painterResource(badge.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (badge.unlocked) 1f else 0.4f),
                contentScale = ContentScale.Fit
            )
            if (!badge.unlocked) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NightBg.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Kilitli",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            badge.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (badge.unlocked) TextCream else TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
