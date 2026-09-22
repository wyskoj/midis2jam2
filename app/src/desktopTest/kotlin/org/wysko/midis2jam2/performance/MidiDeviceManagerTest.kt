/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.performance

import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.manager.MidiDeviceManager
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.NoOpMidiDevice
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the app says to the MIDI device when a performance starts.
 *
 * Two things go out: the specification reset, which puts a device that someone else's song
 * left in a strange state back to a known one, and the synthesizer effect levels, which
 * silence whichever of reverb and chorus the user has turned off.
 */
class MidiDeviceManagerTest {

    @Test
    @Spec("mididevice.spec-reset.sent-on-start")
    fun `the reset message is sent when playback starts`() {
        val device = NoOpMidiDevice()

        withDevice(device, sendReset = true, specification = MidiSpecification.GeneralMidi)

        assertTrue(
            device.messages.any { it.startsWith("data ") },
            "The reset message should have been sent to the device, but it received: ${device.messages}"
        )
    }

    @Test
    fun `no reset message is sent when the option is off`() {
        val device = NoOpMidiDevice()

        withDevice(device, sendReset = false, specification = MidiSpecification.GeneralMidi)

        assertTrue(
            device.messages.none { it.startsWith("data ") },
            "No reset message should be sent when the option is off, but got: ${device.messages}"
        )
    }

    @Test
    fun `each specification sends its own reset message`() {
        val sent = MidiSpecification.entries.associateWith { specification ->
            val device = NoOpMidiDevice()
            withDevice(device, sendReset = true, specification = specification)
            device.messages.singleOrNull { it.startsWith("data ") }
        }

        sent.forEach { (specification, message) ->
            assertTrue(message != null, "No reset message was sent for $specification")
        }
        assertEquals(
            sent.size,
            sent.values.toSet().size,
            "Each MIDI specification should send a different reset message, but got: $sent"
        )
    }

    @Test
    @Spec("synth.effects-sent-as-control-change")
    fun `turning reverb off silences it on every channel`() {
        val device = NoOpMidiDevice()

        withDevice(device) { it.playbackSettings.synthesizerSettings.isUseReverb = false }

        assertEquals(
            (0 until CHANNELS).map { "controlChange ch=$it controller=$REVERB value=0" },
            device.messages,
            "Turning reverb off should send a zero reverb level to every channel"
        )
    }

    @Test
    fun `turning chorus off silences it on every channel`() {
        val device = NoOpMidiDevice()

        withDevice(device) { it.playbackSettings.synthesizerSettings.isUseChorus = false }

        assertEquals(
            (0 until CHANNELS).map { "controlChange ch=$it controller=$CHORUS value=0" },
            device.messages,
            "Turning chorus off should send a zero chorus level to every channel"
        )
    }

    @Test
    fun `leaving an effect on sends nothing, so the soundbank decides`() {
        val device = NoOpMidiDevice()

        withDevice(device) {
            it.playbackSettings.synthesizerSettings.isUseReverb = true
            it.playbackSettings.synthesizerSettings.isUseChorus = true
        }

        assertEquals(
            emptyList(),
            device.messages,
            "An effect that is left on should not be touched at all"
        )
    }

    @Test
    fun `turning both effects off silences both`() {
        val device = NoOpMidiDevice()

        withDevice(device) {
            it.playbackSettings.synthesizerSettings.isUseReverb = false
            it.playbackSettings.synthesizerSettings.isUseChorus = false
        }

        assertEquals(
            CHANNELS * 2,
            device.messages.size,
            "Both effects off means a message per effect per channel"
        )
        assertTrue(device.messages.count { it.contains("controller=$REVERB") } == CHANNELS)
        assertTrue(device.messages.count { it.contains("controller=$CHORUS") } == CHANNELS)
    }

    @Test
    fun `the effect levels are sent after the reset message`() {
        val device = NoOpMidiDevice()

        withDevice(device) {
            it.playbackSettings.midiSpecificationResetSettings.isSendSpecificationResetMessage = true
            it.playbackSettings.synthesizerSettings.isUseReverb = false
        }

        val reset = device.messages.indexOfFirst { it.startsWith("data ") }
        val firstEffect = device.messages.indexOfFirst { it.contains("controller=$REVERB") }

        assertTrue(reset >= 0, "The reset message was not sent")
        assertTrue(
            reset < firstEffect,
            "A specification reset restores the device's default effect levels, so it has to " +
                "go out before the levels this app wants"
        )
    }

    /** Boots a performance with [configure] applied to the settings, recording what the device receives. */
    private fun withDevice(device: NoOpMidiDevice, configure: (AppSettings) -> Unit) {
        val settings = AppSettings().apply(configure)
        attach(device, settings)
    }

    /** Boots a performance with the reset option configured, and a device that records what it receives. */
    private fun withDevice(
        device: NoOpMidiDevice,
        sendReset: Boolean,
        specification: MidiSpecification,
    ) {
        val settings = AppSettings().apply {
            playbackSettings.midiSpecificationResetSettings.isSendSpecificationResetMessage = sendReset
            playbackSettings.midiSpecificationResetSettings.midiSpecification = specification
        }
        attach(device, settings)
    }

    private fun attach(device: NoOpMidiDevice, settings: AppSettings) {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings = settings)
            .use { performance ->
                val configurations = listOf(
                    Configuration.HomeConfiguration(),
                    Configuration.AppSettingsConfiguration(settings),
                )

                // The shipped application attaches this alongside the other managers once the
                // MIDI device is open.
                performance.onEngineThread {
                    performance.app.stateManager.attach(MidiDeviceManager(configurations, device))
                }
                performance.onEngineThread { }
                performance.throwIfEngineFailed()
            }
    }

    private companion object {
        /** Effects 1 and 3 depth: the reverb and chorus send levels. */
        const val REVERB = 91
        const val CHORUS = 93

        const val CHANNELS = 16
    }
}
