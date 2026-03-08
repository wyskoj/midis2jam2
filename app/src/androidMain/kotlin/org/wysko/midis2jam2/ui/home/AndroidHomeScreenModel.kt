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

package org.wysko.midis2jam2.ui.home

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.ExecutionState
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.midi.search.MIDI_FILE_EXTENSIONS
import org.wysko.midis2jam2.midi.system.MidiDevice

class AndroidHomeScreenModel(
    private val applicationService: ApplicationService,
    private val midiService: MidiService,
) : HomeScreenModel {
    private val _selectedMidiFile = MutableStateFlow<PlatformFile?>(null)
    override val selectedMidiFile: StateFlow<PlatformFile?>
        get() = _selectedMidiFile

    override val selectedMidiDevice: StateFlow<MidiDevice>
        get() = MutableStateFlow(getMidiDevices().first())

    override val selectedSoundbank: StateFlow<PlatformFile?>
        get() = MutableStateFlow(null)

    override val isLooping: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    override val isPlayButtonEnabled: Flow<Boolean>
        get() = flowOf(true)

    override val soundbanks: Flow<List<PlatformFile>>
        get() = error("Not supported on Android")

    override fun startApplication() {
        check(selectedMidiFile.value != null) { "MIDI file not set" }

        @Suppress("ReplaceNotNullAssertionWithElvisReturn")
        applicationService.startApplication(
            ExecutionState(
                midiFile = selectedMidiFile.value!!
            )
        )
    }

    override fun setMidiFile(midiFile: PlatformFile?) {
        _selectedMidiFile.value = midiFile
    }

    override fun setMidiDevice(midiDevice: MidiDevice) {
        error("Not supported on Android")
    }

    override fun setSelectedSoundbank(soundbank: PlatformFile?) {
        error("Not supported on Android")
    }

    override fun setLooping(looping: Boolean) {
        error("Not supported on Android")
    }

    override fun getMidiDevices(): List<MidiDevice> {
        return midiService.getMidiDevices()
    }

    @Composable
    override fun midiFilePicker(onFileSelected: ((PlatformFile) -> Unit)?): PickerResultLauncher {
        return rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(),
            title = "Select MIDI file",
        ) { file ->
            _selectedMidiFile.value = file
            file?.let {
                onFileSelected?.invoke(it)
            }
        }
    }

    override fun loadState() {
        error("Not supported on Android")
    }

    override fun saveState() {
        error("Not supported on Android")
    }
}