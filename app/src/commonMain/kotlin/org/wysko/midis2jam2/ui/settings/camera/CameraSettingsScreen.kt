/*
 * Copyright (C) 2025 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.ui.settings.camera

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.camera_video
import midis2jam2.app.generated.resources.motion_photos_auto
import midis2jam2.app.generated.resources.settings_camera
import midis2jam2.app.generated.resources.settings_camera_classic_autocam
import midis2jam2.app.generated.resources.settings_camera_classic_autocam_description
import midis2jam2.app.generated.resources.settings_camera_smooth_freecam
import midis2jam2.app.generated.resources.settings_camera_smooth_freecam_description
import midis2jam2.app.generated.resources.settings_camera_start_autocam_with_song
import midis2jam2.app.generated.resources.settings_camera_start_autocam_with_song_description
import midis2jam2.app.generated.resources.video_stable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel

@Composable
internal expect fun CameraSettingsContent(
    settings: State<AppSettings>,
    settingsModel: SettingsModel,
)

object CameraSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsModel>()
        val settings = settingsModel.appSettings.collectAsState()

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_camera)) }
        ) {
            CameraSettingsContent(settings, settingsModel)
        }
    }
}

@Composable
internal fun ClassicAutocamCard(
    settings: State<AppSettings>,
    settingsModel: SettingsModel,
) {
    SettingsBooleanCard(
        title = { Text(stringResource(Res.string.settings_camera_classic_autocam)) },
        icon = { Icon(painterResource(Res.drawable.camera_video), "") },
        label = { Text(stringResource(Res.string.settings_camera_classic_autocam_description)) },
        isEnabled = settings.value.cameraSettings.isClassicAutoCam,
        setIsEnabled = settingsModel::setClassicAutoCam,
    )
}

@Composable
internal fun SmoothFreecamCard(
    settings: State<AppSettings>,
    settingsModel: SettingsModel,
) {
    SettingsBooleanCard(
        title = { Text(stringResource(Res.string.settings_camera_smooth_freecam)) },
        icon = { Icon(painterResource(Res.drawable.video_stable), "") },
        label = { Text(stringResource(Res.string.settings_camera_smooth_freecam_description)) },
        isEnabled = settings.value.cameraSettings.isSmoothFreecam,
        setIsEnabled = settingsModel::setSmoothFreecam,
    )
}

@Composable
internal fun StartAutocamWithSongCard(
    settings: State<AppSettings>,
    settingsModel: SettingsModel,
) {
    SettingsBooleanCard(
        title = { Text(stringResource(Res.string.settings_camera_start_autocam_with_song)) },
        icon = { Icon(painterResource(Res.drawable.motion_photos_auto), "") },
        label = { Text(stringResource(Res.string.settings_camera_start_autocam_with_song_description)) },
        isEnabled = settings.value.cameraSettings.isStartAutocamWithSong,
        setIsEnabled = settingsModel::setStartAutocamWithSong,
    )
}
