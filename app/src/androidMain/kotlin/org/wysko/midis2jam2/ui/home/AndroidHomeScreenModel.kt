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

import android.net.Uri
import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.BackgroundWarning
import org.wysko.midis2jam2.domain.ExecutionState
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.domain.HomeTabPersistentState
import org.wysko.midis2jam2.domain.HomeTabPersistor
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.ui.settings.SettingsModel
import java.io.File

class AndroidHomeScreenModel(
    private val applicationService: ApplicationService,
    private val midiService: MidiService,
    private val homeTabPersistor: HomeTabPersistor,
    private val settingsModel: SettingsModel,
) : HomeScreenModel {

    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        get() = settingsModel.appSettings.map { appSettings ->
            appSettings.playbackSettings.soundbanksSettings.soundbanks.map { path ->
                PlatformFile(Uri.fromFile(File(path)))
            }
        }

    override val backgroundWarning: Flow<BackgroundWarning?>
        get() = flowOf(null)

    init {
        // When a soundbank is removed from settings, clear the selection if it was the removed one
        modelScope.launch {
            soundbanks.collect { availableSoundbanks ->
                val currentPath = _selectedSoundbank.value?.uri?.path ?: return@collect
                val stillAvailable = availableSoundbanks.any { it.uri?.path == currentPath }
                if (!stillAvailable) {
                    _selectedSoundbank.value = null
                    saveState()
                }
            }
        }
    }

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
        homeTabPersistor.load().soundbank?.let { path ->
            val file = File(path)
            if (file.exists()) {
                _selectedSoundbank.value = PlatformFile(Uri.fromFile(file))
            }
        }
    }

    override fun saveState() {
        homeTabPersistor.save(
            HomeTabPersistentState(
                midiDevice = "", // Android always uses FluidSynth; no device selection to persist
                soundbank = _selectedSoundbank.value?.uri?.path,
            )
        )
    }
}
