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

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.BackgroundWarning
import org.wysko.midis2jam2.domain.ExecutionState
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.domain.HomeTabPersistentState
import org.wysko.midis2jam2.domain.HomeTabPersistor
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.manager.AndroidSoundbanksManager
import org.wysko.midis2jam2.midi.system.MidiDevice
import java.io.File

class AndroidHomeScreenModel(
    private val context: Context,
    private val applicationService: ApplicationService,
    private val midiService: MidiService,
    private val homeTabPersistor: HomeTabPersistor,
    private val soundbankManager: AndroidSoundbanksManager,
) : HomeScreenModel {

    private val _selectedMidiFile = MutableStateFlow<PlatformFile?>(null)
    override val selectedMidiFile: StateFlow<PlatformFile?>
        get() = _selectedMidiFile

    override val selectedMidiDevice: StateFlow<MidiDevice>
        get() = MutableStateFlow(getMidiDevices().first())

    private val _selectedSoundbank = MutableStateFlow<PlatformFile?>(null)
    override val selectedSoundbank: StateFlow<PlatformFile?>
        get() = _selectedSoundbank

    override val isLooping: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    override val isPlayButtonEnabled: Flow<Boolean>
        get() = flowOf(true)

    override val soundbanks: Flow<List<PlatformFile>>
        get() = soundbankManager.soundbanks

    override val backgroundWarning: Flow<BackgroundWarning?>
        get() = flowOf(null)

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
        _selectedSoundbank.value = soundbank
        saveState()
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
            mode = FileKitMode.Single,
            type = FileKitType.File(),
        ) { file ->
            _selectedMidiFile.value = file
            file?.let {
                onFileSelected?.invoke(it)
            }
        }
    }

    override fun loadState() {
        homeTabPersistor.load().soundbank?.let { path ->
            resolveSoundbankFile(path)?.let { file ->
                _selectedSoundbank.value = PlatformFile(Uri.fromFile(file))
            }
        }
    }

    override fun saveState() {
        homeTabPersistor.save(
            HomeTabPersistentState(
                midiDevice = "", // Android always uses FluidSynth; no device selection to persist
                soundbank = _selectedSoundbank.value?.path,
            )
        )
    }

    private fun resolveSoundbankFile(path: String): File? {
        val directFile = File(path)
        if (directFile.exists()) {
            return directFile
        }

        val fallbackFile = File(context.filesDir, "soundbanks/${directFile.name}")
        return fallbackFile.takeIf { it.exists() }
    }
}
