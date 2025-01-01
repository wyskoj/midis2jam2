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
import org.wysko.midis2jam2.starter.configuration.SynthesizerConfiguration

class SynthesizerConfigurationViewModel(
    initConfiguration: SynthesizerConfiguration? = null,
    override val onConfigurationChanged: (SynthesizerConfiguration) -> Unit,
) : ConfigurationViewModel<SynthesizerConfiguration> {
    private val _isReverbEnabled = MutableStateFlow<Boolean>(true)

    /**
     * Whether reverb is enabled.
     */
    val isReverbEnabled
        get() = _isReverbEnabled

    private val _isChorusEnabled = MutableStateFlow<Boolean>(true)

    /**
     * Whether chorus is enabled.
     */
    val isChorusEnabled
        get() = _isChorusEnabled

    /**
     * Sets whether reverb is enabled.
     *
     * @param isEnabled Whether reverb should be enabled.
     */
    fun setReverbEnabled(isEnabled: Boolean) {
        _isReverbEnabled.value = isEnabled
        onConfigurationChanged(generateConfiguration())
    }

    /**
     * Sets whether chorus is enabled.
     *
     * @param isEnabled Whether chorus should be enabled.
     */
    fun setChorusEnabled(isEnabled: Boolean) {
        _isChorusEnabled.value = isEnabled
        onConfigurationChanged(generateConfiguration())
    }

    override fun generateConfiguration(): SynthesizerConfiguration = SynthesizerConfiguration(
        isReverbEnabled = _isReverbEnabled.value,
        isChorusEnabled = _isChorusEnabled.value
    )

    override fun applyConfiguration(configuration: SynthesizerConfiguration) {
        _isReverbEnabled.value = configuration.isReverbEnabled
        _isChorusEnabled.value = configuration.isChorusEnabled
    }

    init {
        initConfiguration?.let { applyConfiguration(it) }
    }

    companion object {
        /**
         * Factory for creating [SynthesizerConfiguration] instances, loading pre-existing configurations if they
         * exist.
         */
        fun create(
            onConfigurationChanged: (SynthesizerConfiguration) -> Unit = {
                SynthesizerConfiguration.preserver.saveConfiguration(it)
            },
        ): SynthesizerConfigurationViewModel =
            SynthesizerConfigurationViewModel(SynthesizerConfiguration.preserver.getConfiguration()) {
                onConfigurationChanged(it)
            }
    }
}
