package com.prompthavenai.falcibuket.ui.screens.flows

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.components.ReadingFlowScaffold
import com.prompthavenai.falcibuket.ui.components.ReadingPreviewNotice
import com.prompthavenai.falcibuket.ui.theme.Gold
import com.prompthavenai.falcibuket.ui.theme.SurfacePlum
import com.prompthavenai.falcibuket.ui.theme.TextCream
import com.prompthavenai.falcibuket.ui.theme.TextMuted

/** Reusable question/text flow. Used by QUESTION_ONLY and TEXT_INPUT types. */
@Composable
fun GenericQuestionReadingScreen(
    title: String,
    subtitle: String,
    prompt: String,
    cta: String,
    @DrawableRes artworkRes: Int?,
    onBack: () -> Unit,
    maxChars: Int = 2000,
    minLines: Int = 3,
    onSubmit: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }

    ReadingFlowScaffold(title = title, subtitle = subtitle, onBack = onBack) {
        if (artworkRes != null) {
            Image(
                painterResource(artworkRes), null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(20.dp))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= maxChars) text = it },
            placeholder = { Text(prompt, color = TextMuted) },
            minLines = minLines,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfacePlum,
                unfocusedContainerColor = SurfacePlum.copy(alpha = 0.6f),
                focusedBorderColor = Gold,
                unfocusedBorderColor = Gold.copy(alpha = 0.3f),
                focusedTextColor = TextCream,
                unfocusedTextColor = TextCream,
                cursorColor = Gold
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        GoldButton(
            cta,
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSubmit(text.trim()) }
        )
        Spacer(Modifier.height(12.dp))
        ReadingPreviewNotice()
    }
}
