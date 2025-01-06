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

package org.wysko.midis2jam2.gui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.starter.configuration.HomeConfiguration
import java.io.File
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem

/**
 * The name of the Gervill MIDI device.
 */
const val GERVILL: String = "Gervill"

/**
 * Represents the ViewModel for the home screen of the application.
 * It provides data and functions related to MIDI devices, soundbanks, and user interactions.
 *
 * @property midiFile The selected MIDI file.
 * @property midiDevices The available MIDI devices.
 * @property selectedMidiDevice The selected MIDI device.
 * @property selectedSoundbank The selected soundbank.
 */
class HomeViewModel(
    initialConfiguration: HomeConfiguration?,
    override val onConfigurationChanged: (HomeConfiguration) -> Unit,
) : ConfigurationViewModel<HomeConfiguration> {
    private val _midiFile = MutableStateFlow<File?>(null)
    val midiFile: StateFlow<File?>
        get() = _midiFile

    private val _midiDevices = MutableStateFlow(getMidiDevices())
    val midiDevices: StateFlow<List<MidiDevice.Info>>
        get() = _midiDevices

    private val _selectedMidiDevice = MutableStateFlow(getMidiDevices().first())
    val selectedMidiDevice: StateFlow<MidiDevice.Info>
        get() = _selectedMidiDevice

    private val _selectedSoundbank = MutableStateFlow<File?>(null)
    val selectedSoundbank: StateFlow<File?>
        get() = _selectedSoundbank

    private val _lastMidiFileSelectedDirectory = MutableStateFlow<String?>(null)
    val lastMidiFileSelectedDirectory: StateFlow<String?>
        get() = _lastMidiFileSelectedDirectory

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean>
        get() = _isLooping

    /**
     * Specifies if the soundbank selector should be shown
     */
    val shouldShowSoundbankSelector: Boolean
        get() = selectedMidiDevice.value.name == GERVILL

    /**
     * Selects a MIDI file for playing.
     *
     * @param file The MIDI file to be selected.
     */
    fun selectMidiFile(file: File?) {
        _midiFile.value = file
        _lastMidiFileSelectedDirectory.value = file?.parent
        onConfigurationChanged(generateConfiguration())
    }

    /**
     * Selects a MIDI device
     *
     * @param device The device to select.
     */
    fun selectMidiDevice(device: MidiDevice.Info) {
        _selectedMidiDevice.value = device
        onConfigurationChanged(generateConfiguration())
    }

    /**
     * Selects a soundbank
     *
     * @param soundbank The soundbank to select.
     */
    fun selectSoundbank(soundbank: File?) {
        _selectedSoundbank.value = soundbank
        onConfigurationChanged(generateConfiguration())
    }

    /**
     * Refreshes the list of available MIDI devices.
     */
    fun refreshMidiDevices() {
        _midiDevices.value = getMidiDevices()
    }

    /**
     * Sets the looping state.
     *
     * @param isLooping The looping state.
     */
    fun setLooping(isLooping: Boolean) {
        _isLooping.value = isLooping
        onConfigurationChanged(generateConfiguration())
    }

    private fun getMidiDevices() =
        runCatching {
            MidiSystem.getMidiDeviceInfo().filter { it.name != "Real Time Sequencer" }
        }.getOrDefault(emptyList())

    override fun generateConfiguration(): HomeConfiguration =
        HomeConfiguration(
            lastMidiFileSelectedDirectory = _lastMidiFileSelectedDirectory.value,
            selectedMidiDevice = _selectedMidiDevice.value.name,
            selectedSoundbank = _selectedSoundbank.value?.absolutePath,
            isLooping = _isLooping.value,
        )

    override fun applyConfiguration(configuration: HomeConfiguration) {
        _lastMidiFileSelectedDirectory.value = configuration.lastMidiFileSelectedDirectory
        _selectedMidiDevice.value =
            _midiDevices.value.firstOrNull { it.name == configuration.selectedMidiDevice } ?: getMidiDevices().first()
        _selectedSoundbank.value = configuration.selectedSoundbank?.let { File(it) }
        _isLooping.value = configuration.isLooping
    }

    init {
        initialConfiguration?.let {
            applyConfiguration(it)
        }
    }

    companion object {
        /** Factory for creating [HomeViewModel] instances, loading pre-existing configurations if they exist. */
        fun create(
            onConfigurationChanged: (HomeConfiguration) -> Unit = {
                HomeConfiguration.preserver.saveConfiguration(it)
            },
        ): HomeViewModel =
            HomeViewModel(HomeConfiguration.preserver.getConfiguration()) {
                onConfigurationChanged(it)
            }
    }
}
