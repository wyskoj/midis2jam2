package org.wysko.midis2jam2.gui.screens.home

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.midi.MidiService
import java.io.File
import javax.sound.midi.MidiDevice

class HomeTabModel(private val midiService: MidiService) : ScreenModel {
    private val _selectedMidiFile = MutableStateFlow<File?>(null)
    val selectedMidiFile: StateFlow<File?>
        get() = _selectedMidiFile

    private val _selectedMidiDevice = MutableStateFlow(getMidiDeviceInfos().first())
    val selectedMidiDevice: StateFlow<MidiDevice.Info>
        get() = _selectedMidiDevice

    private val _availableMidiDevices = MutableStateFlow(getMidiDeviceInfos())
    val availableMidiDevices: StateFlow<List<MidiDevice.Info>>
        get() = _availableMidiDevices

    fun getMidiDeviceSecondaryText(midiDevice: MidiDevice.Info): String = when (midiDevice.vendor) {
        "Unknown vendor" -> midiDevice.description
        else -> "${midiDevice.description} by ${midiDevice.vendor}"
    }

    fun setSelectedMidiFile(file: File) {
        _selectedMidiFile.value = file
    }

    fun setSelectedMidiDevice(midiDevice: MidiDevice.Info) {
        _selectedMidiDevice.value = midiDevice
    }

    @Composable
    fun selectMidiFileLauncher(): PickerResultLauncher {
        return rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(listOf("mid")),
            title = "Select MIDI file",
        ) { file ->
            file?.path?.let {
                setSelectedMidiFile(File(it))
            }
        }
    }

    fun playMidiFile() {

    }

    private fun getMidiDeviceInfos(): List<MidiDevice.Info> =
        midiService.getMidiDeviceInfos().filter { it.name != "Real Time Sequencer" }
}