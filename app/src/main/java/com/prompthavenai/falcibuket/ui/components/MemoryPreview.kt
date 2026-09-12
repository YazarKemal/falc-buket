package com.prompthavenai.falcibuket.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.TextCream

/**
 * Memory Engine önizleme bileşeni. İleride gerçek memory verisiyle beslenecek;
 * şimdilik parametre olarak memories listesi alır (boşsa ilk kullanım durumu).
 */
@Composable
fun MemoryPreviewCard(
    memories: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text("Buket Seni Tanıyor", style = MaterialTheme.typography.titleMedium, color = Gold)
        Spacer(Modifier.height(10.dp))
        if (memories.isEmpty()) {
            MemoryEmptyState()
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pressScale(onClick = {})
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.onboard_memory),
                    contentDescription = null,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        memories.joinToString("\n") { "• $it" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextCream
                    )
                }
            }
        }
    }
}
