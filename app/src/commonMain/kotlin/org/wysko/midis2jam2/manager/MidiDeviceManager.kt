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
import org.wysko.midis2jam2.starter.configuration.PerformanceConfig
import org.wysko.midis2jam2.util.logger

/** The control change that sets a channel's reverb send level. */
private const val CONTROLLER_REVERB_LEVEL = 91

/** The control change that sets a channel's chorus send level. */
private const val CONTROLLER_CHORUS_LEVEL = 93

/** Silences an effect entirely. */
private const val EFFECT_LEVEL_OFF = 0

/** The channels a General MIDI file can use. */
private const val CHANNEL_COUNT = 16

class MidiDeviceManager(
    private val config: PerformanceConfig,
    private val midiDevice: MidiDevice
) : BaseManager() {
    override fun initialize(app: Application) {
        super.initialize(app)
        val isSendResetMessage = config
            .settings
            .playbackSettings
            .midiSpecificationResetSettings
            .isSendSpecificationResetMessage
        if (isSendResetMessage) {
            sendResetMessage()
        }
        // After the reset, which would otherwise restore the device's default effect levels.
        applySynthesizerEffects()
    }

    /**
     * Turns off whichever of reverb and chorus the user has disabled.
     *
     * An effect that is left on is not touched at all, so the soundbank and the file decide
     * how much of it to use. Turning one off means sending its send level to zero on every
     * channel; a file that sets the same control change later in the song will override that,
     * which is why the documentation warns the setting may not always hold.
     */
    private fun applySynthesizerEffects() {
        val synthesizer = config
            .settings
            .playbackSettings
            .synthesizerSettings

        if (!synthesizer.isUseReverb) {
            silenceOnEveryChannel(CONTROLLER_REVERB_LEVEL)
        }
        if (!synthesizer.isUseChorus) {
            silenceOnEveryChannel(CONTROLLER_CHORUS_LEVEL)
        }
    }

    private fun silenceOnEveryChannel(controller: Int) {
        repeat(CHANNEL_COUNT) { channel ->
            midiDevice.sendControlChangeMessage(channel, controller, EFFECT_LEVEL_OFF)
        }
    }

    fun sendResetMessage() {
        val specification = config
            .settings
            .playbackSettings
            .midiSpecificationResetSettings
            .midiSpecification
        midiDevice.sendData(midiSpecificationResetMessage[specification] ?: return)
    }
}