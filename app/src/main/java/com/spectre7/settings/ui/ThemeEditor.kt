package com.spectre7.settings.ui

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.spectre7.composesettings.ui.SettingsPage
import com.spectre7.settings.model.SettingsItem
import com.spectre7.settings.model.SettingsValueState
import com.spectre7.spmp.R
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.spmp.ui.theme.ThemeData
import com.spectre7.utils.*

class SettingsItemThemeSelector<T>(
    val state: SettingsValueState<T>,
    val title: String?,
    val subtitle: String?,
    val editor_title: String?,
    val themeProvider: () -> Theme
): SettingsItem() {
    override fun initialiseValueStates(
        prefs: SharedPreferences,
        default_provider: (String) -> Any,
    ) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Button(
            { openCustomPage(getEditPage(editor_title, themeProvider)) },
            Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.vibrant_accent,
                contentColor = theme.on_accent
            )
        ) {
            Column(Modifier.weight(1f)) {
                if (title != null) {
                    Text(title, color = theme.on_accent)
                }
                ItemText(subtitle, theme.on_accent)
            }
        }
    }
}

private fun getEditPage(
    editor_title: String?,
    themeProvider: () -> Theme
): SettingsPage {
    return object : SettingsPage(editor_title) {
        private var reset by mutableStateOf(false)
        private var pill_extra: (@Composable PillMenu.Action.() -> Unit)? = null

        @Composable
        override fun PageView(
            openPage: (Int) -> Unit,
            openCustomPage: (SettingsPage) -> Unit,
            goBack: () -> Unit,
        ) {
            val ui_theme = settings_interface.theme
            var previewing by remember { mutableStateOf(Theme.preview_active) }

            val icon_button_colours = IconButtonDefaults.iconButtonColors(
                containerColor = ui_theme.vibrant_accent,
                contentColor = ui_theme.vibrant_accent.getContrasted()
            )

            var randomise: Boolean by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (pill_extra == null) {
                    pill_extra =  {
                        val colours = ButtonDefaults.buttonColors(
                            containerColor = background_colour,
                            contentColor = content_colour
                        )

                        Button(
                            { previewing = !previewing },
                            Modifier.fillMaxHeight(),
                            colors = colours,
                            contentPadding = PaddingValues(start = 10.dp, end = 15.dp)
                        ) {
                            Switch(
                                previewing,
                                { previewing = it },
                                Modifier
                                    .scale(0.75f)
                                    .height(0.dp),
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = ui_theme.vibrant_accent.getContrasted(),
                                    checkedThumbColor = ui_theme.vibrant_accent
                                )
                            )
                            Text("Preview", Modifier.padding(start = 5.dp))
                        }

                        Box(
                            Modifier
                                .clickable { randomise = !randomise }
                                .background(background_colour, CircleShape)
                                .clip(CircleShape)
                                .fillMaxHeight()
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_die),
                                null,
                                Modifier.size(25.dp),
                                tint = content_colour
                            )
                        }
                    }
                    settings_interface.pill_menu?.addAlongsideAction(pill_extra!!)
                }
            }

            Crossfade(Pair(themeProvider().theme_data, reset)) { data ->
                val theme = data.first

                var name: String by remember { mutableStateOf(theme.name) }
                var background by remember { mutableStateOf(theme.background) }
                var on_background by remember { mutableStateOf(theme.on_background) }
                var accent by remember { mutableStateOf(theme.accent) }

                OnChangedEffect(previewing) {
                    if (previewing) {
                        Theme.startPreview(ThemeData(name, background, on_background, accent))
                    }
                    else {
                        Theme.stopPreview()
                    }
                }

                val focus_manager = LocalFocusManager.current

                Column(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures {
                                focus_manager.clearFocus()
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        name,
                        { name = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        isError = name.isEmpty(),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = ui_theme.vibrant_accent,
                            cursorColor = ui_theme.vibrant_accent,
                            focusedLabelColor = ui_theme.vibrant_accent
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focus_manager.clearFocus()
                        })
                    )

                    ColourField(
                        "Background",
                        ui_theme,
                        theme.background,
                        icon_button_colours,
                        randomise
                    ) { colour ->
                        background = colour

                        if (Theme.preview_active) {
                            Theme.preview_theme.setBackground(colour)
                        }
                    }
                    ColourField(
                        "On background",
                        ui_theme,
                        theme.on_background,
                        icon_button_colours,
                        randomise
                    ) { colour ->
                        on_background = colour

                        if (Theme.preview_active) {
                            Theme.preview_theme.setOnBackground(colour)
                        }
                    }
                    ColourField(
                        "Accent",
                        ui_theme,
                        theme.accent,
                        icon_button_colours,
                        randomise
                    ) { colour ->
                        accent = colour

                        if (Theme.preview_active) {
                            Theme.preview_theme.setAccent(colour)
                        }
                    }
                }
            }
        }

        override suspend fun resetKeys() {
            themeProvider().setThemeData(Theme.default)
            reset = !reset
        }

        override suspend fun onClosed() {
            super.onClosed()
            Theme.stopPreview()

            if (pill_extra != null) {
                settings_interface.pill_menu?.removeAlongsideAction(pill_extra!!)
                pill_extra = null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
private fun ColourField(
    name: String,
    ui_theme: Theme,
    default_colour: Color,
    button_colours: IconButtonColors,
    randomise: Any,
    onChanged: suspend (Color) -> Unit
) {
    var show_picker by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(default_colour) }
    var instance by remember { mutableStateOf(false) }
    val presets = remember(current) { current.generatePalette(10, 1f).sorted(true) }

    @Composable
    fun Color.presetItem() {
        Spacer(Modifier
            .size(40.dp)
            .background(this, CircleShape)
            .border(Dp.Hairline, contrastAgainst(current), CircleShape)
            .clickable {
                current = this
                instance = !instance
            }
        )
    }

    LaunchedEffect(current) {
        onChanged(current)
    }

    OnChangedEffect(randomise) {
        current = Color.random()
        instance = !instance
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = ui_theme.background.getContrasted())

            Spacer(Modifier
                .fillMaxWidth()
                .weight(1f))

            FilledIconButton({
                show_picker = !show_picker
            }, colors = button_colours) {
                Crossfade(show_picker) { picker ->
                    Icon(if (picker) Icons.Filled.Close else Icons.Filled.Edit, null, Modifier.size(22.dp))
                }
            }
            FilledIconButton({
                current = Color.random()
                instance = !instance
            }, colors = button_colours) {
                Icon(painterResource(R.drawable.ic_die), null, Modifier.size(22.dp))
            }
            FilledIconButton({
                current = default_colour
                instance = !instance
            }, colors = button_colours) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(22.dp))
            }
        }

        val shape = RoundedCornerShape(13.dp)

        Column(Modifier
            .align(Alignment.End)
            .fillMaxWidth()
            .animateContentSize()
            .background(current, shape)
            .border(Dp.Hairline, current.contrastAgainst(ui_theme.background), shape)
            .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val spacing = 10.dp

            Crossfade(show_picker) { picker ->
                if (picker) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        var height by remember { mutableStateOf(0) }
                        LazyColumn(
                            Modifier.height( with(LocalDensity.current) { height.toDp() } ),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            items(presets) { colour ->
                                colour.presetItem()
                            }
                        }
                        Crossfade(instance) {
                            ClassicColorPicker(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .onSizeChanged {
                                        height = it.height
                                    },
                                HsvColor.from(current),
                                showAlphaBar = false
                            ) { colour ->
                                current = colour.toColor()
                            }
                        }
                    }
                }
                else {
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        items(presets) { colour ->
                            colour.presetItem()
                        }
                    }
                }
            }
        }
    }
}
