package org.wysko.midis2jam2.gui.screens.settings.playback.soundbanks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.remove
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.settings.AppModel
import java.io.File

object SoundbanksSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val soundbanks by app.playback.soundbanks.soundbanks.collectAsState()

        val soundbankLauncher = rememberFilePickerLauncher(
            mode = PickerMode.Multiple(),
            type = PickerType.File(listOf("sf2", "dls")),
            title = "Select soundbanks",
            onResult = { files ->
                files?.forEach { file ->
                    file.path?.let {
                        app.playback.soundbanks.addSoundbank(File(it))
                    }
                }
            }
        )

        SettingsScreenSkeleton("Soundbanks") {
            Button(onClick = { soundbankLauncher.launch() }) {
                Text("Add soundbanks")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (soundbanks.size) {
                    0 -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(256.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No soundbanks loaded", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    else -> items(soundbanks) { soundbank ->
                        Card(Modifier.height(64.dp).fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(soundbank.name, style = MaterialTheme.typography.titleMedium)
                                    Text(soundbank.parent, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { app.playback.soundbanks.removeSoundbanks(setOf(soundbank)) }
                                ) {
                                    Icon(painterResource(Res.drawable.remove), "")
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}