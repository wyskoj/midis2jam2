/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.testing

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.wysko.midis2jam2.domain.BackgroundWarning
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.midi.system.MidiDevice

/**
 * A home screen that holds state and does nothing else.
 *
 * The real desktop one opens the MIDI subsystem while it constructs, which a test has no
 * business doing; this stands in wherever something merely reads the user's choices.
 */
class FakeHomeScreenModel(
    midiFile: PlatformFile? = null,
    soundbank: PlatformFile? = null,
    isLooping: Boolean = false,
    private val devices: List<MidiDevice> = listOf(NoOpMidiDevice("Gervill")),
) : HomeScreenModel {

    override val selectedMidiFile = MutableStateFlow(midiFile)
    override val selectedMidiDevice = MutableStateFlow(devices.first())
    override val selectedSoundbank = MutableStateFlow(soundbank)
    override val isLooping = MutableStateFlow(isLooping)

    override val isPlayButtonEnabled: Flow<Boolean> = selectedMidiFile.map { it != null }
    override val soundbanks: Flow<List<PlatformFile>> = MutableStateFlow(emptyList())
    override val backgroundWarning: Flow<BackgroundWarning?> = MutableStateFlow(null)

    /** How many times a performance was asked for. */
    var startCount: Int = 0
        private set

    override fun startApplication() {
        startCount++
    }

    override fun setMidiFile(midiFile: PlatformFile?) {
        selectedMidiFile.value = midiFile
    }

    override fun setMidiDevice(midiDevice: MidiDevice) {
        selectedMidiDevice.value = midiDevice
    }

    override fun setSelectedSoundbank(soundbank: PlatformFile?) {
        selectedSoundbank.value = soundbank
    }

    override fun setLooping(looping: Boolean) {
        this.isLooping.value = looping
    }

    override fun getMidiDevices(): List<MidiDevice> = devices

    @Composable
    override fun midiFilePicker(onFileSelected: ((PlatformFile) -> Unit)?): PickerResultLauncher =
        error("A test never opens a file dialog")

    override fun loadState() = Unit

    override fun saveState() = Unit
}
