package org.wysko.midis2jam2.gui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.gui.components.settings.card.ListPosition
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardGroupNavigate
import org.wysko.midis2jam2.gui.screens.settings.background.BackgroundSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.camera.CameraSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.controls.ControlsSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.general.GeneralSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.graphics.GraphicsSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.onscreenelements.OnScreenElementsSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.playback.PlaybackSettingsScreen

object MainSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current!!
        val settingsScreens = listOf(
            listOf(
                SettingsScreen(
                    "General",
                    "Language, theme",
                    painterResource(Res.drawable.settings_outline),
                    GeneralSettingsScreen
                ),
                SettingsScreen(
                    "Graphics",
                    "Fullscreen, screen resolution, anti-aliasing",
                    painterResource(Res.drawable.display_settings),
                    GraphicsSettingsScreen
                ),
                SettingsScreen(
                    "Background",
                    "Choose a picture or color",
                    painterResource(Res.drawable.wallpaper),
                    BackgroundSettingsScreen
                )
            ),
            listOf(
                SettingsScreen(
                    "Controls",
                    "Cursor lock, speed modifier keys",
                    painterResource(Res.drawable.keyboard),
                    ControlsSettingsScreen
                ),
                SettingsScreen(
                    "Playback",
                    "MIDI device, synthesizer, soundbanks",
                    painterResource(Res.drawable.media_output),
                    PlaybackSettingsScreen
                ),
                SettingsScreen(
                    "On-screen elements",
                    "Heads-up display, lyrics",
                    painterResource(Res.drawable.screenshot_monitor),
                    OnScreenElementsSettingsScreen
                )
            ),
            listOf(
                SettingsScreen(
                    "Camera",
                    "Autocam, smooth camera, classic camera",
                    painterResource(Res.drawable.videocam),
                    CameraSettingsScreen
                )
            )
        )

        LazyColumn(
            verticalArrangement = spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            item {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            items(settingsScreens) { group ->
                Column(verticalArrangement = spacedBy(2.dp)) {
                    group.forEachIndexed { index, it ->
                        SettingsCardGroupNavigate(
                            title = it.title,
                            description = { it.description },
                            icon = it.icon,
                            listPosition = ListPosition.fromIndex(index, group.size)
                        ) {
                            navigator.push(it.screen!!)
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsScreen(
    val title: String,
    val description: String,
    val icon: Painter,
    val screen: Screen? = null // TODO remove nullable
)

