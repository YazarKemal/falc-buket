package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.data.model.FortuneCategory
import com.prompthavenai.falcibuket.data.model.FortuneType
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.FortuneCategorySection
import com.prompthavenai.falcibuket.ui.model.FortuneCatalog
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.NightBg
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextCream
import com.prompthavenai.falcibuket.ui.theme.TextMuted

private const val FILTER_ALL = "all"
private const val FILTER_POPULAR = "popular"

@Composable
fun FortuneCatalogScreen(nav: NavController) {
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    val onOpen: (FortuneType) -> Unit = { nav.navigate(Routes.openFortune(it)) }

    Box(
        Modifier.fillMaxSize().background(NightBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            Modifier.widthIn(max = 1100.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
                    }
                    Column {
                        Text("Tüm Fallar", style = MaterialTheme.typography.headlineMedium, color = Gold)
                        Text(
                            "28 fal türünü keşfet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { CatalogChip("Tümü", filter == FILTER_ALL) { filter = FILTER_ALL } }
                    item { CatalogChip("Popüler", filter == FILTER_POPULAR) { filter = FILTER_POPULAR } }
                    items(FortuneCategory.entries) { category ->
                        CatalogChip(
                            label = category.title,
                            selected = filter == category.name,
                            onClick = { filter = category.name }
                        )
                    }
                }
            }

            when {
                filter == FILTER_POPULAR -> item {
                    FortuneCategorySection("Popüler", FortuneCatalog.popular, onOpen)
                }

                filter == FILTER_ALL -> {
                    FortuneCategory.entries.forEach { category ->
                        val types = FortuneCatalog.forCategory(category)
                        if (types.isNotEmpty()) {
                            item(key = category.name) {
                                FortuneCategorySection(category.title, types, onOpen)
                            }
                        }
                    }
                }

                else -> {
                    val category = FortuneCategory.entries.firstOrNull { it.name == filter }
                    val types = category?.let { FortuneCatalog.forCategory(it) }.orEmpty()
                    if (category != null) {
                        item(key = category.name) {
                            FortuneCategorySection(category.title, types, onOpen)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun CatalogChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfacePlum,
            labelColor = TextMuted,
            selectedContainerColor = Gold,
            selectedLabelColor = Color(0xFF2A1A05)
        )
    )
}
