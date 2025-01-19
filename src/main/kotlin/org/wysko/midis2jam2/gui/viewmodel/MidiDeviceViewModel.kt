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
import org.wysko.midis2jam2.instrument.algorithmic.assignment.MidiSpecification
import org.wysko.midis2jam2.starter.configuration.MidiDeviceConfiguration

class MidiDeviceViewModel(
    initConfiguration: MidiDeviceConfiguration? = null,
    override val onConfigurationChanged: (MidiDeviceConfiguration) -> Unit,
) : ConfigurationViewModel<MidiDeviceConfiguration> {
    private val _isSendResetMessage = MutableStateFlow(false)

    val isSendResetMessage
        get() = _isSendResetMessage

    fun setIsSendResetMessage(isSendResetMessage: Boolean) {
        _isSendResetMessage.value = isSendResetMessage
        onConfigurationChanged(generateConfiguration())
    }

    private val _resetMessageSpecification = MutableStateFlow<MidiSpecification>(MidiSpecification.GeneralMidi)

    val resetMessageSpecification
        get() = _resetMessageSpecification

    fun setResetMessageSpecification(resetMessageSpecification: MidiSpecification) {
        _resetMessageSpecification.value = resetMessageSpecification
        onConfigurationChanged(generateConfiguration())
    }

    override fun generateConfiguration(): MidiDeviceConfiguration {
        return MidiDeviceConfiguration(
            isSendResetMessage = isSendResetMessage.value,
            resetMessageSpecification = resetMessageSpecification.value,
        )
    }

    override fun applyConfiguration(configuration: MidiDeviceConfiguration) {
        _isSendResetMessage.value = configuration.isSendResetMessage
        _resetMessageSpecification.value = configuration.resetMessageSpecification
    }

    init {
        initConfiguration?.let { applyConfiguration(it) }
    }

    companion object {
        /**
         * Factory for creating [MidiDeviceViewModel] instances, loading pre-existing configurations if they
         * exist.
         */
        fun create(
            onConfigurationChanged: (MidiDeviceConfiguration) -> Unit = {
                MidiDeviceConfiguration.preserver.saveConfiguration(it)
            },
        ): MidiDeviceViewModel =
            MidiDeviceViewModel(MidiDeviceConfiguration.preserver.getConfiguration()) {
                onConfigurationChanged(it)
            }
    }
}