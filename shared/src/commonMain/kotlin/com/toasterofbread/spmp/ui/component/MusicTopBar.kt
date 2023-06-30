package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.MusicTopBarMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.toasterofbread.utils.composable.rememberSongUpdateLyrics
import com.toasterofbread.utils.getContrasted
import kotlinx.coroutines.delay

private fun getModeState(mode: MusicTopBarMode, song: Song?): Any? {
    return when (mode) {
        MusicTopBarMode.LYRICS -> song?.lyrics?.lyrics?.let {  lyrics ->
            if (lyrics.synced) lyrics else null
        }
        MusicTopBarMode.VISUALISER -> mode
    }
}

@Composable
private fun isStateActive(state: Any, can_show_visualiser: Boolean): Boolean = when (state) {
    is SongLyrics -> true
    MusicTopBarMode.VISUALISER -> can_show_visualiser && LocalPlayerState.current.status.m_playing
    else -> false
}

@Composable
fun MusicTopBarWithVisualiser(
    target_mode_key: Settings,
    modifier: Modifier = Modifier,
    song: Song? = LocalPlayerState.current.status.m_song,
    can_show_visualiser: Boolean = false,
    hide_while_inactive: Boolean = true,
    padding: PaddingValues = PaddingValues(),
    onShowingChanged: ((Boolean) -> Unit)? = null
) {
    var target_mode: MusicTopBarMode by target_mode_key.rememberMutableEnumState()
    val show_toast = remember { mutableStateOf(false) }

    MusicTopBar(
        { target_mode },
        true,
        can_show_visualiser,
        hide_while_inactive,
        modifier,
        song,
        padding,
        innerContent = { mode ->
            Crossfade(Pair(target_mode, mode), Modifier.fillMaxSize()) { state ->
                val (target, current) = state

                val toast_alpha = remember { Animatable(if (show_toast.value) 1f else 0f) }
                LaunchedEffect(Unit) {
                    if (!show_toast.value) {
                        return@LaunchedEffect
                    }

                    show_toast.value = false
                    delay(500)
                    toast_alpha.animateTo(0f)
                }

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .graphicsLayer { alpha = toast_alpha.value }
                            .background(LocalContentColor.current, RoundedCornerShape(16.dp)),
                    ) {
                        Row(
                            Modifier.padding(vertical = 5.dp, horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val colour = LocalContentColor.current.getContrasted()
                            Icon(
                                target.getIcon(),
                                null,
                                tint = colour
                            )

                            if (target != current) {
                                Text(getString("topbar_mode_unavailable"), color = colour)
                            }
                        }
                    }
                }
            }
        },
        onClick = {
            target_mode = target_mode.getNext(can_show_visualiser)
            show_toast.value = true
        },
        onShowingChanged = onShowingChanged
    )
}

@Composable
fun MusicTopBar(
    can_show_key: Settings,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    onShowingChanged: ((Boolean) -> Unit)? = null
) {
    val can_show: Boolean by can_show_key.rememberMutableState()
    MusicTopBar(
        { MusicTopBarMode.LYRICS },
        can_show = can_show,
        can_show_visualiser = false,
        hide_while_inactive = true,
        modifier = modifier,
        padding = padding,
        onShowingChanged = onShowingChanged
    )
}

@Composable
private fun MusicTopBar(
    getTargetMode: () -> MusicTopBarMode,
    can_show: Boolean,
    can_show_visualiser: Boolean,
    hide_while_inactive: Boolean,
    modifier: Modifier = Modifier,
    song: Song? = LocalPlayerState.current.status.m_song,
    padding: PaddingValues = PaddingValues(),
    innerContent: (@Composable (MusicTopBarMode) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onShowingChanged: ((Boolean) -> Unit)? = null
) {
    val player = LocalPlayerState.current
    val song_state by rememberSongUpdateLyrics(song, getTargetMode() == MusicTopBarMode.LYRICS)
    var mode_state: MusicTopBarMode by mutableStateOf(getTargetMode())

    val visualiser_width: Float by Settings.KEY_TOPBAR_VISUALISER_WIDTH.rememberMutableState()
    check(visualiser_width in 0f .. 1f)

    val current_state by remember {
        derivedStateOf {
            val target = getTargetMode()
            for (mode_i in target.ordinal downTo 0) {
                val mode = MusicTopBarMode.values()[mode_i]
                val state = getModeState(mode, song_state)
                if (state != null) {
                    mode_state = mode
                    return@derivedStateOf state
                }
            }
            throw NotImplementedError(target.toString())
        }
    }

    val show = !hide_while_inactive || isStateActive(current_state, can_show_visualiser)
    DisposableEffect(show) {
        onShowingChanged?.invoke(show)
        onDispose {
            onShowingChanged?.invoke(false)
        }
    }

    AnimatedVisibility(
        show,
        modifier.platformClickable(
            onClick = onClick,
            onAltClick = {
                if (current_state is SongLyrics) {
                    player.openNowPlayingOverlayMenu(OverlayMenu.getLyricsMenu())
                }
            }
        ),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(Modifier.padding(padding).height(30.dp), contentAlignment = Alignment.Center) {
            innerContent?.invoke(mode_state)

            Crossfade(current_state, Modifier.fillMaxSize()) { s ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (s) {
                        is SongLyrics -> {
                            val linger: Boolean by Settings.KEY_TOPBAR_LYRICS_LINGER.rememberMutableState()
                            val show_furigana: Boolean by Settings.KEY_TOPBAR_LYRICS_SHOW_FURIGANA.rememberMutableState()

                            LyricsLineDisplay(
                                s,
                                {
                                    (player.player?.current_position_ms ?: 0) +
                                        (song?.song_reg_entry?.getLyricsSyncOffset() ?: 0)
                                },
                                linger,
                                show_furigana
                            )
                        }
                        MusicTopBarMode.VISUALISER -> {
                            player.player?.Visualiser(
                                LocalContentColor.current,
                                Modifier.fillMaxHeight().fillMaxWidth(visualiser_width).padding(vertical = 10.dp),
                                opacity = 0.5f
                            )
                        }
                        else -> {
                            // TOOD State indicator
                        }
                    }
                }
            }
        }
    }
}