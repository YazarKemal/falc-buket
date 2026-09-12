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
import androidx.compose.material3.TextButton
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

/** Small secondary promo card; intentionally less dominant than the daily hero. */
@Composable
fun NightPromoCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfacePlum)
            .pressScale(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(R.drawable.promo_small_mystic_card),
            contentDescription = null,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Bu gece için özel yorum",
                style = MaterialTheme.typography.titleMedium,
                color = Gold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Bugünün enerjisine daha yakından bak.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        TextButton(onClick = onClick) {
            Text("Keşfet", color = Gold)
        }
    }
}
