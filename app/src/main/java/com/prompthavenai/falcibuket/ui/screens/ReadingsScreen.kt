package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.components.pressScale
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun ReadingsScreen() {
    val entries = listOf(
        Triple("Bugünün Falı", "Bugün", R.drawable.result_background),
        Triple("Aşk Falı", "2 gün önce", R.drawable.love_fortune),
        Triple("Kahve Falı", "1 hafta önce", R.drawable.coffee_fortune)
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Image(
                painterResource(R.drawable.history_header), null,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
            Text("Fallarım", style = MaterialTheme.typography.headlineMedium, color = Gold)
        }
        items(entries) { (title, date, img) ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfacePlum)
                    .pressScale(onClick = {})
                    .padding(14.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Image(
                    painterResource(img), null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextCream)
                    Text(date, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
        }
    }
}
