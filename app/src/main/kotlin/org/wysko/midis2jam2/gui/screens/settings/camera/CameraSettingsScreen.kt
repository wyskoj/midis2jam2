package org.wysko.midis2jam2.gui.screens.settings.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.autoplay
import midis2jam2.app.generated.resources.camera_video
import midis2jam2.app.generated.resources.video_stable
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.settings.AppModel

object CameraSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val isStartAutocamWithSong by app.camera.isStartAutocamWithSong.collectAsState()
        val isSmoothFreecam by app.camera.isSmoothFreecam.collectAsState()
        val isClassicAutocam by app.camera.isClassicAutocam.collectAsState()

        SettingsScreenSkeleton("Camera") {
            SettingsCardBoolean(
                isChecked = isStartAutocamWithSong,
                onValueChange = app.camera::setIsStartAutocamWithSong,
                title = "Start autocam with song",
                icon = painterResource(Res.drawable.autoplay),
                description = { "Automatically start the autocam when the song begins" }
            )
            SettingsCardBoolean(
                isChecked = isSmoothFreecam,
                onValueChange = app.camera::setIsSmoothFreecam,
                title = "Smooth freecam",
                icon = painterResource(Res.drawable.video_stable),
                description = { "Make freecam motion smoother" }
            )
            SettingsCardBoolean(
                isChecked = isClassicAutocam,
                onValueChange = app.camera::setIsClassicAutocam,
                title = "Classic autocam",
                icon = painterResource(Res.drawable.camera_video),
                description = { "Simulates autocam movement from MIDIJam" }
            )
        }
    }
}