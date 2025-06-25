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
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.ExecutionState
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.domain.isInternal
import org.wysko.midis2jam2.midi.system.MidiDevice

class HomeTabModel(
    private val midiService: MidiService,
    private val applicationService: ApplicationService,
) : ScreenModel, KoinComponent {
    private val _state = MutableStateFlow(
        HomeTabState(
            selectedMidiDevice = midiService.getMidiDevices().first()
        )
    )
    val state: StateFlow<HomeTabState>
        get() = _state

    val isApplicationRunning: StateFlow<Boolean>
        get() = applicationService.isApplicationRunning

    @Composable
    fun midiFilePicker(
        extraCallback: ((PlatformFile?) -> Unit)? = null
    ): PickerResultLauncher {
        return rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(listOf("mid")),
            title = "Select MIDI file",
        ) { file ->
            _state.value = _state.value.copy(selectedMidiFile = file)
            extraCallback?.invoke(file)
        }
    }

    fun startApplication() {
        check(_state.value.selectedMidiFile != null) { "MIDI file not set" }

        with(_state.value) {
            applicationService.startApplication(
                ExecutionState(
                    midiFile = selectedMidiFile!!
                )
            )
        }
    }

    fun getMidiDevices(): List<MidiDevice> {
        return midiService.getMidiDevices()
    }

    fun setSelectedMidiDevice(midiDevice: MidiDevice) {
        _state.value = _state.value.copy(selectedMidiDevice = midiDevice)
    }

    fun getSoundbanks(): List<PlatformFile> {
        return emptyList() // TODO
    }

    fun setSelectedSoundbank(soundbank: PlatformFile?) {
        _state.value = _state.value.copy(
            selectedSoundbank = soundbank
        )
    }

    fun isShowSoundbankSelector(): Boolean = _state.value.selectedMidiDevice.isInternal()
}
