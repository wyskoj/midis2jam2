package org.wysko.midis2jam2.gui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.ExposedDropDownMenu
import org.wysko.midis2jam2.gui.components.Midis2jam2Logo
import org.wysko.midis2jam2.settings.AppModel
import java.io.File
import javax.sound.midi.MidiDevice

object HomeTab : Tab {
    override val key: ScreenKey = "home"

    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(index = 0u, title = "Home", icon = null)
        }

    @Composable
    override fun Content() {
        val appViewModel = koinViewModel<AppModel>()
        val soundbanks by appViewModel.playback.soundbanks.soundbanks.collectAsState()
        val selectedSoundbank by appViewModel.selectedSoundbank.collectAsState()

        val homeTabModel = koinScreenModel<HomeTabModel>()
        val selectedFile by homeTabModel.selectedMidiFile.collectAsState()
        val selectedMidiDevice by homeTabModel.selectedMidiDevice.collectAsState()
        val availableMidiDevices by homeTabModel.availableMidiDevices.collectAsState()
        val selectMidiFileLauncher = homeTabModel.selectMidiFileLauncher()

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(512.dp)
            ) {
                Midis2jam2Logo()
                MidiFileTextField(selectedFile, selectMidiFileLauncher)
                MidiDeviceDropDown(
                    availableMidiDevices,
                    selectedMidiDevice,
                    homeTabModel::getMidiDeviceSecondaryText,
                    homeTabModel::setSelectedMidiDevice
                )
                AnimatedVisibility(visible = selectedMidiDevice.name == "Gervill") {
                    SoundbankDropDown(
                        soundbanks,
                        selectedSoundbank,
                        appViewModel::setSelectedSoundbank
                    )
                }
            }
        }
    }
}

@Composable
private fun MidiFileTextField(selectedFile: File?, selectMidiFileLauncher: PickerResultLauncher) {
    TextField(
        value = selectedFile?.name ?: "",
        onValueChange = {},
        label = { Text("MIDI file") },
        singleLine = true,
        readOnly = true,
        trailingIcon = { MidiFileBrowseButton { selectMidiFileLauncher.launch() } },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MidiDeviceDropDown(
    availableMidiDevices: List<MidiDevice.Info>,
    selectedMidiDevice: MidiDevice.Info,
    getMidiDeviceSecondaryText: (MidiDevice.Info) -> String,
    setSelectedMidiDevice: (MidiDevice.Info) -> Unit
) {
    ExposedDropDownMenu(
        items = availableMidiDevices,
        selectedItem = selectedMidiDevice,
        title = "MIDI device",
        displayText = { it.name },
        secondaryText = { getMidiDeviceSecondaryText(it) },
        onItemSelected = { setSelectedMidiDevice(it) }
    )
}

@Composable
private fun SoundbankDropDown(
    loadedSoundbanks: List<File>,
    selectedSoundbank: File?,
    setSelectedSoundbank: (File?) -> Unit
) {
    ExposedDropDownMenu(
        items = listOf(null) + loadedSoundbanks,
        selectedItem = selectedSoundbank,
        title = "MIDI device",
        displayText = { it?.name ?: "Default soundbank" },
        secondaryText = { it?.parent ?: "The default soundbank provided by the synthesizer" },
        onItemSelected = { setSelectedSoundbank(it) }
    )
}

@Composable
private fun MidiFileBrowseButton(onClick: () -> Unit) {
    IconButton(onClick, Modifier.pointerHoverIcon(PointerIcon.Hand)) {
        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Browse")
    }
}