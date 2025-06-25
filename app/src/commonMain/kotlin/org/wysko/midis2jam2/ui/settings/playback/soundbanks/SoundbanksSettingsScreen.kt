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

package org.wysko.midis2jam2.ui.settings.playback.soundbanks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.remove
import midis2jam2.app.generated.resources.settings_playback_soundbanks
import midis2jam2.app.generated.resources.settings_playback_soundbanks_add
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel
import java.io.File

object SoundbanksSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsModel>()
        val settings = settingsModel.appSettings.collectAsState()

        val soundbankLauncher = rememberFilePickerLauncher(
            mode = PickerMode.Multiple(),
            type = PickerType.File(listOf("sf2", "dls")),
            title = "Select soundbanks",
            onResult = { files ->
                if (files != null) {
                    settingsModel.addSoundbanks(files.map { it.path!! })
                }
            }
        )

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_playback_soundbanks)) }
        ) {
            Button(onClick = soundbankLauncher::launch) {
                Text(stringResource(Res.string.settings_playback_soundbanks_add))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (settings.value.playbackSettings.soundbanksSettings.soundbanks.size) {
                    0 -> Box(
                        modifier = Modifier.Companion.fillMaxWidth().padding(16.dp).height(256.dp),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        Text("No soundbanks loaded", style = MaterialTheme.typography.bodyMedium)
                    }

                    else -> settings.value.playbackSettings.soundbanksSettings.soundbanks.forEach { soundbank ->
                        val asFile = File(soundbank)
                        SoundbankCard(asFile) { settingsModel.removeSoundbank(soundbank) }
                    }
                }
            }
        }
    }

    @Composable
    private fun SoundbankCard(
        asFile: File,
        removeSoundbank: () -> Unit,
    ) {
        Card(Modifier.Companion.height(64.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.Companion.fillMaxHeight().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Column {
                    Text(asFile.name, style = MaterialTheme.typography.titleMedium)
                    Text(asFile.parent, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.Companion.weight(1f))
                IconButton(onClick = removeSoundbank) {
                    Icon(painterResource(Res.drawable.remove), "")
                }
            }
        }
    }
}