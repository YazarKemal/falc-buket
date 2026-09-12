package com.prompthavenai.falcibuket.ui.components

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Sessiz, otomatik oynayan ve döngüde kalan mistik arka plan videosu.
 *
 * - Ses kapalıdır (ses parçası hiç çözülmez), kontroller gizlidir.
 * - Yaşam düşkünlüğüne duyarlıdır: arka planda çözmez, öne gelince sürer.
 * - Composition'dan çıkınca player serbest bırakılır (sızıntı yok).
 * - Kurulum VEYA oynatma hatasında [fallback] (varsa) gösterilir; çökmez.
 *
 * Player `remember(videoRes)` ile bir kez oluşturulur; yeniden kompozisyon
 * player'ı yeniden yaratmaz.
 */
@OptIn(UnstableApi::class)
@Composable
fun MysticLoopVideo(
    @RawRes videoRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var failed by remember(videoRes) { mutableStateOf(false) }

    val player = remember(videoRes) {
        runCatching { buildLoopingPlayer(context, videoRes) }.getOrNull()
    }

    if (player == null || failed) {
        // Kurulum veya zaman uyumsuz oynatma hatası: sessizce statik içeriğe dön.
        fallback?.invoke()
        return
    }

    DisposableEffect(player, lifecycleOwner) {
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        player.addListener(errorListener)
        // Yaşam döngüsü zaten STOPPED olabilir; başlangıçta arka planda çözme.
        player.playWhenReady = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> player.playWhenReady = true
                Lifecycle.Event.ON_STOP -> player.playWhenReady = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(errorListener)
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                resizeMode = if (contentScale == ContentScale.Crop) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                this.player = player
            }
        },
        update = { it.player = player }
    )
}

@UnstableApi
private fun buildLoopingPlayer(context: Context, @RawRes resId: Int): ExoPlayer {
    val uri = Uri.parse("android.resource://${context.packageName}/$resId")
    return ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(uri))
        repeatMode = Player.REPEAT_MODE_ALL
        volume = 0f
        // Ses parçasını hiç çözme/çalma: garantili sessizlik ve daha hafif CPU.
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        playWhenReady = true
        prepare()
    }
}
