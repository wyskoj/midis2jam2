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

package org.wysko.midis2jam2.midi

import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.midi.system.MidiDevice

val midiSpecificationResetMessage: Map<MidiSpecification, ByteArray> = mapOf(
    MidiSpecification.GeneralMidi to byteArrayOf(
        0xF0.toByte(),
        0x7E.toByte(),
        0x7F.toByte(),
        0x09.toByte(),
        0x01.toByte(),
        0xF7.toByte(),
    ),
    MidiSpecification.ExtendedGeneral to byteArrayOf(
        0xF0.toByte(),
        0x43.toByte(),
        0x10.toByte(),
        0x4C.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x7E.toByte(),
        0x00.toByte(),
        0xF7.toByte(),
    ),
    MidiSpecification.GeneralStandard to byteArrayOf(
        0xF0.toByte(),
        0x41.toByte(),
        0x10.toByte(),
        0x42.toByte(),
        0x12.toByte(),
        0x40.toByte(),
        0x00.toByte(),
        0x7F.toByte(),
        0x00.toByte(),
        0x41.toByte(),
        0xF7.toByte(),
    ),
)

fun MidiDevice.sendResetMessage(specification: MidiSpecification) {
    val message = midiSpecificationResetMessage[specification]
    check(message != null) { "Unsupported MIDI specification: $specification" }
    sendData(message)
}