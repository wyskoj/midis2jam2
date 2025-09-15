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
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.midi_device
import midis2jam2.app.generated.resources.midi_file
import midis2jam2.app.generated.resources.play
import midis2jam2.app.generated.resources.play_arrow
import midis2jam2.app.generated.resources.repeat
import midis2jam2.app.generated.resources.repeat_on
import midis2jam2.app.generated.resources.soundbank
import midis2jam2.app.generated.resources.soundbank_default
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.midi.search.MIDI_FILE_EXTENSIONS
import org.wysko.midis2jam2.ui.common.component.Midis2jam2Logo
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel
import org.wysko.midis2jam2.ui.home.log.LogScreenButton
import org.wysko.midis2jam2.util.FileDragAndDrop

@Composable
internal actual fun HomeScreenLayout() {
    val model = koinInject<HomeScreenModel>()
    val navigationModel = koinInject<NavigationModel>()
    val applyHomeScreenMidiFile = navigationModel.applyHomeScreenMidiFile.collectAsState()
    val scope = rememberCoroutineScope()
    var flicker by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.currentOrThrow

    LaunchedEffect(Unit) {
        model.loadState()
    }

    val selectedMidiDevice = model.selectedMidiDevice.collectAsState()
    val selectedSoundbank = model.selectedSoundbank.collectAsState()
    val isLooping = model.isLooping.collectAsState()

    LaunchedEffect(
        selectedMidiDevice.value,
        selectedSoundbank.value,
        isLooping.value,
    ) {
        model.saveState()
    }

    LaunchedEffect(applyHomeScreenMidiFile) {
        applyHomeScreenMidiFile.value?.let { file ->
            scope.launch {
                delay(100)
                model.setMidiFile(PlatformFile(file))
                navigationModel.clearApplyHomeScreenMidiFile()
                flicker = true
                delay(200)
                flicker = false
            }
        }
    }

    val dragAndDropTarget = remember {
        FileDragAndDrop {
            if (MIDI_FILE_EXTENSIONS.contains(it.extension.lowercase())) {
                model.setMidiFile(PlatformFile(it))
            }
        }
    }

    val isSoundbankSelectVisible = selectedMidiDevice.value.name == "Gervill"

    Scaffold(
        modifier = Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = dragAndDropTarget,
        )
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            LogScreenButton(navigator, Modifier.align(Alignment.TopEnd).padding(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 512.dp).padding(horizontal = 16.dp).fillMaxHeight()
                    .align(Alignment.Center)
            ) {
                item {
                    Midis2jam2Logo()
                }
                item {
                    MidiFilePicker(model, flicker)
                }
                item {
                    MidiDeviceSelector(model)
                }
                item {
                    SoundbankSelector(model, isSoundbankSelectVisible)
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayButton(model, model::startApplication)
                        LoopButton(model)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SoundbankSelector(
    model: HomeScreenModel,
    isVisible: Boolean,
) {
    val selectedSoundbank = model.selectedSoundbank.collectAsState()
    val soundbanks = model.soundbanks.collectAsState(initial = emptyList())

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        SoundbankSelectorImpl(
            soundbanks = soundbanks.value,
            selectedSoundbank = selectedSoundbank.value,
        ) { soundbank ->
            model.setSelectedSoundbank(soundbank)
        }
    }
}

@Composable
internal fun MidiDeviceSelector(
    model: HomeScreenModel,
) {
    val selectedMidiDevice = model.selectedMidiDevice.collectAsState()
    val midiDevices = model.getMidiDevices()

    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { },
    ) {
        TextFieldDefaults.DecorationBox(
            selectedMidiDevice.value.name,
            innerTextField = {
                Text(selectedMidiDevice.value.name, modifier = Modifier.fillMaxWidth())
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
                        model.setMidiDevice(deviceInfo)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun MidiFilePicker(
    model: HomeScreenModel,
    flicker: Boolean = false,
) {
    val selectedMidiFile = model.selectedMidiFile.collectAsState()
    val midiFilePicker = model.midiFilePicker(null)
    val flickerSize by animateFloatAsState(if (flicker) 1.05f else 1f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
    ) {
        MidiFileSelector(
            selectedMidiFile = selectedMidiFile.value?.name ?: "",
            modifier = Modifier.weight(1f).scale(flickerSize)
        ) {
            midiFilePicker.launch()
        }
    }
}

@Composable
fun PlayButton(
    model: HomeScreenModel,
    onClick: () -> Unit,
) {
    val isEnabled = model.isPlayButtonEnabled.collectAsState(false)
    Button(
        onClick = onClick,
        modifier = Modifier.width(192.dp).height(56.dp),
        enabled = isEnabled.value,
    ) {
        Icon(painterResource(Res.drawable.play_arrow), "", modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.play), fontSize = 16.sp)
    }
}

@Composable
fun LoopButton(model: HomeScreenModel) {
    val isLooping = model.isLooping.collectAsState()
    IconToggleButton(
        checked = isLooping.value,
        onCheckedChange = { model.setLooping(it) }
    ) {
        val drawable = when (isLooping.value) {
            true -> Res.drawable.repeat_on
            false -> Res.drawable.repeat
        }
        Icon(painterResource(drawable), null)
    }
}

@Composable
internal fun SoundbankSelectorImpl(
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
