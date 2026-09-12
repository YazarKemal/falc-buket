package com.prompthavenai.falcibuket.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prompthavenai.falcibuket.R
import com.prompthavenai.falcibuket.data.model.ChatMessage
import com.prompthavenai.falcibuket.ui.components.MysticBackground
import com.prompthavenai.falcibuket.ui.theme.*
import com.prompthavenai.falcibuket.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(nav: NavController) {
    val vm: ChatViewModel = viewModel()
    val state by vm.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.sending) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bubbleMax = minOf(maxWidth * 0.82f, 520.dp)
        MysticBackground(R.drawable.bg_mystic_soft_01) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 960.dp).fillMaxSize().imePadding()) {
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

                    state.error?.let { err ->
                        ErrorBanner(
                            message = err.userMessage,
                            onRetry = if (state.messages.any { it.failed }) vm::retryLastFailed else null
                        )
                    }

                    LazyColumn(
                        Modifier.weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.loading && state.messages.isEmpty()) {
                            item { Text("Buket geliyor…", color = TextMuted, style = MaterialTheme.typography.bodyMedium) }
                        }
                        items(state.messages) { msg -> MessageBubble(msg, bubbleMax) }
                        if (state.sending) {
                            item { TypingIndicator() }
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
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                vm.send(input)
                                input = ""
                            },
                            enabled = input.isNotBlank() && !state.sending
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, "Gönder",
                                tint = if (input.isNotBlank() && !state.sending) Gold else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, maxWidth: Dp) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (msg.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = if (msg.fromUser) 20.dp else 4.dp,
                        bottomEnd = if (msg.fromUser) 4.dp else 20.dp
                    )
                )
                .background(if (msg.fromUser) GoldDeep else SurfacePlum)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(if (msg.sending) 0.6f else 1f)
        ) {
            Text(msg.text, color = TextCream, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        0.25f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot"
    )
    Box(
        Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(SurfacePlum)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text("Buket düşünüyor…", color = TextMuted.copy(alpha = alpha), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Rose.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = Rose, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("Tekrar dene", color = Gold) }
        }
    }
}
