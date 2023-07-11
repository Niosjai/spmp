package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.api.getOrReport
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.*
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.*
import kotlinx.coroutines.*
import org.burnoutcrew.reorderable.*
import com.toasterofbread.spmp.ui.component.mediaitempreview.getSongLongPressMenuData
import com.toasterofbread.utils.modifier.background

@Composable
fun PlaylistPage(
    pill_menu: PillMenu,
    playlist: Playlist,
    previous_item: MediaItem? = null,
    padding: PaddingValues = PaddingValues(),
    close: () -> Unit
) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val multiselect_context = remember { MediaItemMultiSelectContext() { context ->
        if (playlist.is_editable != true) {
            return@MediaItemMultiSelectContext
        }

        IconButton({ coroutine_scope.launch {
            val items = context.getSelectedItems().sortedByDescending { it.second!! }
            for (item in items) {
                playlist.removeItem(item.second!!)
                context.setItemSelected(item.first, false, item.second)
            }
            playlist.saveItems().getOrReport("PlaylistPageRemoveItemSave")
        } }) {
            Icon(Icons.Default.PlaylistRemove, null)
        }
    } }

    var accent_colour: Color? by remember { mutableStateOf(null) }
    var reorderable: Boolean by remember { mutableStateOf(false) }
    var current_filter: String? by remember { mutableStateOf(null) }
    var current_sort_option: SortOption by remember { mutableStateOf(SortOption.PLAYLIST) }
    val top_padding = padding.calculateTopPadding()

    LaunchedEffect(playlist) {
        accent_colour = null
        playlist.getFeedLayouts().getOrReport("PlaylistPageLoad")
    }

    Column(Modifier.fillMaxSize()) {
        if (previous_item != null) {
            Row(Modifier.fillMaxWidth().padding(top = top_padding), verticalAlignment = Alignment.CenterVertically) {
                IconButton(close) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))
                previous_item.title!!.also { Text(it) }
                Spacer(Modifier.fillMaxWidth().weight(1f))

                IconButton({ player.showLongPressMenu(previous_item) }) {
                    Icon(Icons.Default.MoreVert, null)
                }
            }
        }

        val thumb_item = playlist.getThumbnailHolder().getHolder()

        LaunchedEffect(thumb_item) {
            if (thumb_item == playlist) {
                accent_colour = playlist.getThemeColour() ?: Theme.current.accent
            }
        }

        val sorted_items: MutableList<Pair<MediaItem, Int>> = remember { mutableStateListOf() }
        LaunchedEffect(playlist.items?.size, current_sort_option, current_filter) {
            sorted_items.clear()
            playlist.items?.let { items ->
                val filtered_items = current_filter.let { filter ->
                    if (filter == null) items
                    else items.filter { it.title!!.contains(filter, true) }
                }

                sorted_items.addAll(
                    current_sort_option
                        .sortItems(filtered_items)
                        .mapIndexed { index, value ->
                            Pair(value, index)
                        }
                )
            }
        }

        OnChangedEffect(reorderable) {
            if (!reorderable) {
                playlist.saveItems().getOrReport("PlaylistPageSaveItems")
            }
        }

        val items_above = 2
        val list_state = rememberReorderableLazyListState(
            onMove = { from, to ->
                check(reorderable)
                check(current_filter == null)
                check(current_sort_option == SortOption.PLAYLIST)

                if (to.index >= items_above && from.index >= items_above) {
                    sorted_items.add(to.index - items_above, sorted_items.removeAt(from.index - items_above))
                }
            },
            onDragEnd = { from, to ->
                if (to >= items_above && from >= items_above) {
                    playlist.moveItem(from - items_above, to - items_above)
                }
            }
        )

        var editing_info by remember { mutableStateOf(false) }
        val horizontal_padding = SpMp.context.getDefaultHorizontalPadding()

        val final_padding by MultiselectAndMusicTopBar(
            multiselect_context,
            Modifier.fillMaxWidth(),
            show_wave_border = false,
            padding = PaddingValues(
                top = if (previous_item != null) 0.dp else top_padding,
                start = horizontal_padding,
                end = horizontal_padding
            )
        )

        LazyColumn(
            state = list_state.listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.reorderable(list_state),
            contentPadding = final_padding
        ) {
            item {
                PlaylistTopInfo(
                    playlist,
                    accent_colour ?: Theme.current.accent,
                    editing_info,
                    { editing_info = it }
                ) {
                    accent_colour = thumb_item.item?.getThemeColour()
                }
            }

            item {
                PlaylistButtonBar(
                    playlist,
                    accent_colour ?: Theme.current.accent,
                    editing_info,
                    { editing_info = it }
                )
            }

            playlist.items?.also { items ->
                stickyHeaderWithTopPadding(
                    list_state.listState,
                    final_padding.calculateTopPadding(),
                    Modifier.zIndex(1f),
                    Theme.current.background_provider
                ) {
                    InteractionBar(
                        modifier = Modifier.fillMaxWidth(),
                        playlist = playlist,
                        items = items,
                        reorderable = reorderable,
                        setReorderable = {
                            reorderable = playlist.is_editable == true && it
                            if (reorderable) {
                                current_sort_option = SortOption.PLAYLIST
                                current_filter = null
                            }
                        },
                        filter = current_filter,
                        setFilter = {
                            check(!reorderable)
                            current_filter = it
                        },
                        sort_option = current_sort_option,
                        setSortOption = {
                            check(!reorderable)
                            current_sort_option = it
                        },
                        multiselect_context = multiselect_context
                    )
                }

                PlaylistItems(
                    playlist,
                    items,
                    list_state,
                    sorted_items,
                    multiselect_context,
                    reorderable,
                    current_sort_option,
                    player,
                )
            }
        }
    }
}

private fun LazyListScope.PlaylistItems(
    playlist: Playlist,
    items: MutableList<MediaItem>,
    list_state: ReorderableLazyListState,
    sorted_items: List<Pair<MediaItem, Int>>,
    multiselect_context: MediaItemMultiSelectContext,
    reorderable: Boolean,
    sort_option: SortOption,
    player: PlayerState
) {
    if (sorted_items.isEmpty()) {
        item {
            Text(getString("playlist_empty"), Modifier.padding(top = 15.dp))
        }
    }

    items(sorted_items, key = { it.second }) {
        val (item, index) = it
        check(item is Song)

        val long_press_menu_data = remember(item) {
            getSongLongPressMenuData(
                item,
                multiselect_context = multiselect_context,
                multiselect_key = index
            )
        }

        ReorderableItem(list_state, key = index) { dragging ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .mediaItemPreviewInteraction(
                        item,
                        long_press_menu_data,
                        onClick = { item, index ->
                            player.playPlaylist(playlist, index!!)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(50.dp)) {
                    item.Thumbnail(
                        MediaItemThumbnailProvider.Quality.LOW,
                        Modifier.fillMaxSize().longPressMenuIcon(long_press_menu_data)
                    )
                    multiselect_context.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = index)
                }

                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        item.title!!,
                        style = MaterialTheme.typography.titleSmall
                    )

                    val duration_text = remember(item.duration) {
                        item.duration?.let { duration -> durationToString(duration, true, hl = SpMp.ui_language) }
                    }
                    duration_text?.also { text ->
                        Text(text, style = MaterialTheme.typography.labelSmall)
                    }
                }

                AnimatedVisibility(reorderable) {
                    Icon(Icons.Default.Reorder, null, Modifier.padding(end = 20.dp).detectReorder(list_state))
                }
            }
        }
    }
}

internal enum class SortOption {
    PLAYLIST, ALPHABET, DURATION, PLAY_COUNT;

    fun getReadable(): String =
        getString(when(this) {
            PLAYLIST ->   "playlist_sort_option_playlist"
            ALPHABET ->   "playlist_sort_option_alphabet"
            DURATION ->   "playlist_sort_option_duration"
            PLAY_COUNT -> "playlist_sort_option_playcount"
        })

    fun sortItems(items: List<MediaItem>, reversed: Boolean = false): List<MediaItem> {
        val selector: (MediaItem) -> Comparable<*> = when (this) {
            PLAYLIST ->
                return if (reversed) items.asReversed()
                else items
            ALPHABET -> {
                { it.title!! }
            }
            DURATION -> {
                { if (it is Song) it.duration ?: 0 else 0 }
            }
            PLAY_COUNT -> {
                { it.registry_entry.getPlayCount(null) }
            }
        }
        return items.sortedWith(if (reversed) compareByDescending(selector) else compareBy(selector))
    }
}
