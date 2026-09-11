package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.theme.*

private data class Tab(val label: String, val icon: ImageVector, val selected: Color)

@Composable
fun MainScreen(nav: NavController) {
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab("Ana Sayfa", Icons.Outlined.AutoAwesome, Gold),
        Tab("Fallarım", Icons.Outlined.History, Rose),
        Tab("Buket", Icons.Outlined.Spa, Gold),
        Tab("Profil", Icons.Outlined.Person, Rose)
    )
    Column(Modifier.fillMaxSize().background(NightBg)) {
        Box(Modifier.weight(1f)) {
            when (selected) {
                0 -> HomeScreen(nav)
                1 -> ReadingsScreen()
                2 -> ChatScreen(nav)
                else -> ProfileScreen()
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfacePlum.copy(alpha = 0.85f))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == selected
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selected = i }
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon, tab.label,
                        tint = if (active) tab.selected else MutedPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) tab.selected else MutedPurple
                    )
                }
            }
        }
    }
}

// Muted grey-purple for inactive tabs
private val MutedPurple = Color(0xFF8B7BA3)
