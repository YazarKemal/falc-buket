package com.prompthavenai.falcibuket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextMuted

/**
 * Honest provenance label for mock/preview readings. Never present demo data as
 * a real analysis.
 */
@Composable
fun ReadingPreviewNotice(
    modifier: Modifier = Modifier,
    detail: String = "Bu içerik demo verisidir; verdiğin bilgilerle gerçek bir analiz yapılmadı."
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfacePlum.copy(alpha = 0.6f))
            .padding(12.dp)
    ) {
        Text("Önizleme · Örnek yorum", style = MaterialTheme.typography.labelMedium, color = Gold)
        Spacer(Modifier.height(4.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
