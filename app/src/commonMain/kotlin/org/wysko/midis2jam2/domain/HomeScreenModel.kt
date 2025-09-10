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

package org.wysko.midis2jam2.domain

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.midi.system.MidiDevice

interface HomeScreenModel {
    val selectedMidiFile: StateFlow<PlatformFile?>
    val selectedMidiDevice: StateFlow<MidiDevice>
    val selectedSoundbank: StateFlow<PlatformFile?>
    val isLooping: StateFlow<Boolean>
    val isPlayButtonEnabled: Flow<Boolean>
    val soundbanks: Flow<List<PlatformFile>>

    fun startApplication()

    fun setMidiFile(midiFile: PlatformFile?)
    fun setMidiDevice(midiDevice: MidiDevice)
    fun setSelectedSoundbank(soundbank: PlatformFile?)
    fun setLooping(looping: Boolean)

    fun getMidiDevices(): List<MidiDevice>

    @Composable
    fun midiFilePicker(onFileSelected: ((PlatformFile) -> Unit)?): PickerResultLauncher

    fun loadState()
    fun saveState()
}