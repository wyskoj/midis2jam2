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
import org.wysko.midis2jam2.starter.configuration.SoundbankConfiguration
import java.io.File

/**
 * ViewModel class for managing soundbank configuration.
 */
class SoundBankConfigurationViewModel(
    initialConfiguration: SoundbankConfiguration? = null,
    override val onConfigurationChanged: (SoundbankConfiguration) -> Unit
) : ConfigurationViewModel<SoundbankConfiguration> {
    private val _soundbanks: MutableStateFlow<Set<File>> = MutableStateFlow(
        setOf(

        )
    )

    /**
     * The list of soundbanks.
     */
    val soundbanks: StateFlow<Set<File>>
        get() = _soundbanks

    /**
     * Attempts to add a soundbank to the list of soundbanks. It first checks if the soundbank is valid.
     *
     * @param soundbank The soundbank to add.
     */
    fun addSoundbank(soundbank: String) {
        val file = File(soundbank)
        if (!SoundbankConfiguration.isValidSoundbankFile(file)) return
        _soundbanks.value += file
        onConfigurationChanged(generateConfiguration())
    }

    /**
     * Removes a soundbank from the list of soundbanks.
     *
     * @param soundbank The soundbank to remove.
     */
    fun removeSoundbank(soundbank: File) {
        _soundbanks.value -= soundbank
        onConfigurationChanged(generateConfiguration())
    }

    override fun generateConfiguration(): SoundbankConfiguration = SoundbankConfiguration(soundbanks.value)

    override fun applyConfiguration(configuration: SoundbankConfiguration) {
        _soundbanks.value = configuration.soundbanks
    }

    init {
        initialConfiguration?.let { applyConfiguration(it) }
    }

    companion object {
        /** Factory for creating [SoundBankConfigurationViewModel] instances, loading pre-existing configurations if they exist. */
        fun create(
            onConfigurationChanged: (SoundbankConfiguration) -> Unit = {
                SoundbankConfiguration.preserver.saveConfiguration(it)
            }
        ): SoundBankConfigurationViewModel = SoundBankConfigurationViewModel(SoundbankConfiguration.preserver.getConfiguration()) {
            onConfigurationChanged(it)
        }
    }
}
