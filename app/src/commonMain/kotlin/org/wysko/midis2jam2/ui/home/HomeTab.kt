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

@file:OptIn(ExperimentalMaterial3Api::class)

package org.wysko.midis2jam2.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.core.PlatformFile
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.midi_device
import midis2jam2.app.generated.resources.midi_file
import midis2jam2.app.generated.resources.midis2jam2_logo
import midis2jam2.app.generated.resources.play
import midis2jam2.app.generated.resources.play_arrow
import midis2jam2.app.generated.resources.soundbank
import midis2jam2.app.generated.resources.soundbank_default
import midis2jam2.app.generated.resources.tab_home
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.inject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.midi.system.MidiDevice
import kotlin.getValue

@Composable
internal expect fun HomeTabLayout(
    state: State<HomeTabState>,
    midiFilePicker: PickerResultLauncher,
    model: HomeTabModel,
    isApplicationRunning: State<Boolean>,
)

@Composable
internal fun SoundbankSelector(
    model: HomeTabModel,
    state: State<HomeTabState>,
) {
    val isShowSoundbankSelector = model.isShowSoundbankSelector.collectAsState()
    val soundbanks = model.soundbanks.collectAsState(initial = emptyList())
    AnimatedVisibility(
        visible = isShowSoundbankSelector.value,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        SoundbankSelector(
            soundbanks = soundbanks.value,
            selectedSoundbank = state.value.selectedSoundbank,
        ) { soundbank ->
            model.setSelectedSoundbank(soundbank)
        }
    }
}

@Composable
internal fun MidiDeviceSelector(
    model: HomeTabModel,
    state: State<HomeTabState>,
) {
    MidiDeviceSelector(
        midiDevices = model.getMidiDevices(),
        selectedMidiDevice = state.value.selectedMidiDevice,
    ) { deviceInfo ->
        model.setSelectedMidiDevice(deviceInfo)
    }
}

@Composable
internal fun MidiFilePicker(
    state: State<HomeTabState>,
    midiFilePicker: PickerResultLauncher,
    flicker: Boolean = false,
) {
    val flickerSize by animateFloatAsState(if (flicker) 1.05f else 1f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MidiFileSelector(
            selectedMidiFile = state.value.selectedMidiFile?.name,
            modifier = Modifier.weight(1f).scale(flickerSize)
        ) {
            midiFilePicker.launch()
        }
    }
}

@Composable
internal fun Midis2jam2Logo() {
    Icon(
        painter = painterResource(Res.drawable.midis2jam2_logo),
        contentDescription = "midis2jam2",
        modifier = Modifier.height(128.dp)
    )
}

@Composable
fun PlayButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(192.dp).height(56.dp),
        enabled = enabled
    ) {
        Icon(painterResource(Res.drawable.play_arrow), "", modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.play), fontSize = 16.sp)
    }
}

@Composable
internal fun SoundbankSelector(
    soundbanks: List<PlatformFile>,
    selectedSoundbank: PlatformFile? = null,
    modifier: Modifier = Modifier,
    onSelectSoundbank: (PlatformFile?) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val selectedSoundbankName = selectedSoundbank?.name ?: stringResource(Res.string.soundbank_default)
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { },
        modifier = Modifier.then(modifier),
    ) {
        TextFieldDefaults.DecorationBox(
            selectedSoundbankName,
            innerTextField = {
                Text(selectedSoundbankName, modifier = Modifier.fillMaxWidth())
            },
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text(stringResource(Res.string.soundbank)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = isExpanded
                )
            },
            container = {
                TextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).clickable {
                        isExpanded = !isExpanded
                    }
                )
            }
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            (listOf<PlatformFile?>(null) + soundbanks).forEach { soundbank ->
                DropdownMenuItem(
                    text = { Text(soundbank?.name ?: stringResource(Res.string.soundbank_default)) },
                    onClick = {
                        onSelectSoundbank(soundbank)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun MidiFileSelector(
    selectedMidiFile: String? = null,
    modifier: Modifier = Modifier,
    onOpenMidiFilePicker: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.clickable {
            onOpenMidiFilePicker()
        }.then(modifier)
    ) {
        TextFieldDefaults.DecorationBox(
            selectedMidiFile ?: "",
            innerTextField = {
                Text(selectedMidiFile ?: "", Modifier.fillMaxWidth())
            },
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text(stringResource(Res.string.midi_file)) },
        )
    }
}

@Composable
internal fun MidiDeviceSelector(
    midiDevices: List<MidiDevice>,
    selectedMidiDevice: MidiDevice,
    modifier: Modifier = Modifier,
    onMidiDeviceSelected: (MidiDevice) -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { },
        modifier = Modifier.then(modifier),
    ) {
        TextFieldDefaults.DecorationBox(
            selectedMidiDevice.name,
            innerTextField = {
                Text(selectedMidiDevice.name, modifier = Modifier.fillMaxWidth())
            },
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text(stringResource(Res.string.midi_device)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = isExpanded
                )
            },
            container = {
                TextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).clickable {
                        isExpanded = !isExpanded
                    }
                )
            }
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            midiDevices.forEach { deviceInfo ->
                DropdownMenuItem(
                    text = { Text(deviceInfo.name) },
                    onClick = {
                        onMidiDeviceSelected(deviceInfo)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

object MainHomeScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<HomeTabModel>()
        val midiFilePicker = model.midiFilePicker()
        val state = model.state.collectAsState()
        val isApplicationRunning = model.isApplicationRunning.collectAsState()
        HomeTabLayout(state, midiFilePicker, model, isApplicationRunning)
    }
}

object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(Res.string.tab_home),
            icon = null,
        )

    @Composable
    override fun Content() {
        Navigator(MainHomeScreen) { navigator ->
            FadeTransition(navigator) { screen ->
                screen.Content()
            }
        }
    }
}
