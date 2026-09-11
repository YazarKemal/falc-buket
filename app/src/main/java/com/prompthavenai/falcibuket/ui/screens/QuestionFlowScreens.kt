package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.navigation.Routes
import com.prompthavenai.falcibuket.ui.components.GoldButton
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun LoveScreen(nav: NavController) = QuestionScreen(
    nav = nav,
    imageRes = R.drawable.love_fortune,
    title = "Kalbindeki Soruyu Sor",
    prompt = "Aşk hayatında neyi merak ediyorsun?",
    cta = "Aşk Falıma Bak",
    type = "Aşk"
)

@Composable
fun CareerScreen(nav: NavController) = QuestionScreen(
    nav = nav,
    imageRes = R.drawable.career_fortune,
    title = "Önündeki Yolu Keşfet",
    prompt = "Kariyer veya para konusunda neyi merak ediyorsun?",
    cta = "Kariyer Falıma Bak",
    type = "Kariyer"
)

@Composable
private fun QuestionScreen(
    nav: NavController,
    imageRes: Int,
    title: String,
    prompt: String,
    cta: String,
    type: String
) {
    var question by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(NightBg).verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        IconButton(onClick = { nav.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Gold)
        Spacer(Modifier.height(20.dp))
        androidx.compose.foundation.Image(
            painterResource(imageRes), null,
            modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(24.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            placeholder = { Text(prompt, color = TextMuted) },
            minLines = 3,
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
        Spacer(Modifier.height(24.dp))
        GoldButton(
            cta,
            enabled = question.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = { nav.navigate(Routes.result(type)) }
        )
        Spacer(Modifier.height(20.dp))
    }
}
