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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.ExecutionState
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.domain.HomeTabPersistentState
import org.wysko.midis2jam2.domain.HomeTabPersistor
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.ui.settings.SettingsModel
import java.io.File

class DesktopHomeScreenModel(
    private val applicationService: ApplicationService,
    private val homeTabPersistor: HomeTabPersistor,
    private val midiService: MidiService,
) : HomeScreenModel, KoinComponent {
    private val _selectedMidiFile = MutableStateFlow<PlatformFile?>(null)
    override val selectedMidiFile: StateFlow<PlatformFile?>
        get() = _selectedMidiFile

    private val _selectedMidiDevice = MutableStateFlow(getMidiDevices().first())
    override val selectedMidiDevice: StateFlow<MidiDevice>
        get() = _selectedMidiDevice

    private val _selectedSoundbank = MutableStateFlow<PlatformFile?>(null)
    override val selectedSoundbank: StateFlow<PlatformFile?>
        get() = _selectedSoundbank

    private val _isLooping = MutableStateFlow(false)
    override val isLooping: StateFlow<Boolean>
        get() = _isLooping

    override val isPlayButtonEnabled: Flow<Boolean>
        get() = _selectedMidiFile.combine(applicationService.isApplicationRunning) { midiFile, isRunning ->
            !isRunning && midiFile != null
        }

    override val soundbanks: Flow<List<PlatformFile>> = run {
        val settings: SettingsModel by inject()
        settings.appSettings.map { settings ->
            settings.playbackSettings.soundbanksSettings.soundbanks.map { PlatformFile(File(it)) }
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

    override fun setSelectedSoundbank(soundbank: PlatformFile?) {
        _selectedSoundbank.value = soundbank
    }

    override fun setMidiDevice(midiDevice: MidiDevice) {
        _selectedMidiDevice.value = midiDevice
    }

    override fun setLooping(looping: Boolean) {
        _isLooping.value = looping
    }

    override fun getMidiDevices(): List<MidiDevice> = midiService.getMidiDevices()

    @Composable
    override fun midiFilePicker(
        onFileSelected: ((PlatformFile) -> Unit)?,
    ): PickerResultLauncher {
        return rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(listOf("mid")),
            title = "Select MIDI file",
        ) { file ->
            _selectedMidiFile.value = file
            file?.let {
                onFileSelected?.invoke(it)
            }
        }
    }

    override fun loadState() {
        homeTabPersistor.load().run {
            _selectedSoundbank.value = PlatformFile(File(soundbank))
            getMidiDevices().find { it.name == midiDevice }?.let {
                _selectedMidiDevice.value = it
            }
        }
    }

    override fun saveState() {
        homeTabPersistor.save(
            HomeTabPersistentState(
                midiDevice = _selectedMidiDevice.value.name,
                soundbank = _selectedSoundbank.value?.path ?: "",
            )
        )
    }
}