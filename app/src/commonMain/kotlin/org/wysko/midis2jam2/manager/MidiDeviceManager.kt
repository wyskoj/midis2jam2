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

package org.wysko.midis2jam2.manager

import com.jme3.app.Application
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.midi.midiSpecificationResetMessage
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.Configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.util.logger

class MidiDeviceManager(
    private val configs: Collection<Configuration>,
    private val midiDevice: MidiDevice
) : BaseManager() {
    override fun initialize(app: Application) {
        super.initialize(app)
        val isSendResetMessage = configs
            .find<AppSettingsConfiguration>()
            .appSettings
            .playbackSettings
            .midiSpecificationResetSettings
            .isSendSpecificationResetMessage
        if (isSendResetMessage) {
            sendResetMessage()
        }
    }

    fun sendResetMessage() {
        val specification = configs
            .find<AppSettingsConfiguration>()
            .appSettings
            .playbackSettings
            .midiSpecificationResetSettings
            .midiSpecification
        midiDevice.sendData(midiSpecificationResetMessage[specification] ?: return)
    }
}