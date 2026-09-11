package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.model.ChatMessage
import com.prompthavenai.falcibuket.ui.theme.*

@Composable
fun ChatScreen(nav: NavController) {
    var input by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Hoş geldin. Bugün özellikle aşk, kariyer, para veya hayatındaki başka bir konu hakkında mı konuşmak istiyorsun?",
                false
            )
        )
    }
    Column(Modifier.fillMaxSize().background(NightBg).imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = TextCream)
            }
            Image(
                painterResource(R.drawable.teller_avatar), "Buket",
                modifier = Modifier.size(44.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("FalcıBuket", style = MaterialTheme.typography.titleMedium, color = TextCream)
                Text("Senin için burada ✨", style = MaterialTheme.typography.labelMedium, color = Gold)
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (msg.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Box(
                        Modifier
                            .widthIn(max = 300.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp, topEnd = 20.dp,
                                    bottomStart = if (msg.fromUser) 20.dp else 4.dp,
                                    bottomEnd = if (msg.fromUser) 4.dp else 20.dp
                                )
                            )
                            .background(if (msg.fromUser) GoldDeep else SurfacePlum)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(msg.text, color = if (msg.fromUser) TextCream else TextCream, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfacePlum)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Buket'e bir şey anlat...", color = TextMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ColorTransparent,
                    unfocusedContainerColor = ColorTransparent,
                    focusedIndicatorColor = ColorTransparent,
                    unfocusedIndicatorColor = ColorTransparent
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        messages.add(ChatMessage(input.trim(), true))
                        input = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Gönder", tint = Gold)
            }
        }
    }
}

private val ColorTransparent = androidx.compose.ui.graphics.Color.Transparent
