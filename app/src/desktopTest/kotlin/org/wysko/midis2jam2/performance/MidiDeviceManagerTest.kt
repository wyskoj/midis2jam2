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
 * The reset message the app can send to an external MIDI device when playback begins.
 *
 * The point of the message is to put a device that someone else's song left in a strange
 * state back to a known one, so it has to go out at the start and match the chosen standard.
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
}
