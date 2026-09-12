package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.SimpleGrid
import com.prompthavenai.falcibuket.ui.components.adaptiveColumns
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun TarotScreen(nav: NavController) {
    var selected by remember { mutableStateOf(setOf<Int>()) }

    Box(Modifier.fillMaxSize().background(NightBg), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1000.dp).fillMaxSize()) {
            val columns = adaptiveColumns(
                availableWidth = maxWidth,
                minCellWidth = 110.dp,
                spacing = 12.dp,
                max = 6
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
                }
                Text("Kartlarını Seç", style = MaterialTheme.typography.headlineMedium, color = Gold)
                Spacer(Modifier.height(8.dp))
                Text("İçinden geldiği gibi üç kart seç. (${selected.size}/3)", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                Image(
                    painterResource(R.drawable.tarot_selection), null,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(24.dp))
                SimpleGrid(
                    items = (0..5).toList(),
                    columns = columns,
                    horizontalSpacing = 12.dp,
                    verticalSpacing = 14.dp
                ) { i ->
                    TarotCard(i, selected, Modifier.fillMaxWidth()) { toggle(selected, i) { selected = it } }
                }
                Spacer(Modifier.height(28.dp))
                GoldButton(
                    "Kartlarımı Yorumla",
                    enabled = selected.size == 3,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { nav.navigate(Routes.result("Tarot")) }
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun toggle(selected: Set<Int>, i: Int, set: (Set<Int>) -> Unit) {
    set(
        when {
            i in selected -> selected - i
            selected.size < 3 -> selected + i
            else -> selected
        }
    )
}

@Composable
private fun TarotCard(index: Int, selected: Set<Int>, modifier: Modifier, onClick: () -> Unit) {
    val isSelected = index in selected
    val scale by animateFloatAsState(if (isSelected) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "card")
    Box(
        modifier
            .aspectRatio(0.62f)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.verticalGradient(
                    if (isSelected) listOf(Gold, GoldDeep) else listOf(SurfacePlum, Color(0xFF2C1B45))
                )
            )
            .border(
                1.dp,
                if (isSelected) Color.White.copy(alpha = 0.7f) else Gold.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.tarot_fortune), null,
            modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
            alpha = if (isSelected) 1f else 0.55f
        )
    }
}
