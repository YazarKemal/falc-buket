package com.prompthavenai.falcibuket.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextMuted

/**
 * Reusable empty state for "Buket Seni Tanıyor". Later this will be driven by
 * the actual memory count.
 */
@Composable
fun MemoryEmptyState(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfacePlum)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.empty_state_memory_building),
            contentDescription = null,
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(360f / 640f)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Henüz yeni tanışıyoruz",
                style = MaterialTheme.typography.titleMedium,
                color = Gold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Bana anlattıkça önemli detayları hatırlayacağım ve sonraki falların daha kişisel olacak.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}
