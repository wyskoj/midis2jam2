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

package org.wysko.midis2jam2.instrument.algorithmic.assignment

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A MIDI specification.
 *
 * @property name The name of the MIDI specification.
 * @property initialism The initialism (abbreviation) of the MIDI specification.
 * @property resetMessage The data that should be sent to reset the MIDI device to this specification.
 */
@Serializable
sealed class MidiSpecification(
    val name: String,
    val initialism: String,
    val resetMessage: ByteArray
) {

    /** GM. */
    data object GeneralMidi : MidiSpecification(
        "General MIDI",
        "GM",
        byteArrayOf(
            0xF0.toByte(),
            0x7E.toByte(),
            0x7F.toByte(),
            0x09.toByte(),
            0x01.toByte(),
            0xF7.toByte(),
        )
    )

    /** Roland GS. */
    data object GeneralStandardMidi : MidiSpecification(
        "Roland General Standard",
        "GS",
        byteArrayOf(
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

    /** Yamaha XG. */
    data object ExtendedGeneralMidi : MidiSpecification(
        "Yamaha Extended General",
        "XG",
        byteArrayOf(
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
    )
}

/**
 * An object that serializes and deserializes [MidiSpecification] objects to and from their string representations.
 */
object MidiSpecificationSerializer : KSerializer<MidiSpecification> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MidiSpecification", PrimitiveKind.STRING)

    /**
     * Serialize a [MidiSpecification] into a string.
     *
     * @param encoder [Encoder] to aid the serialization.
     * @param value [MidiSpecification] object to serialize.
     */
    override fun serialize(encoder: Encoder, value: MidiSpecification): Unit = encoder.encodeString(value.initialism)

    /**
     * Deserialize a string into a [MidiSpecification].
     *
     * @param decoder [Decoder] to aid the deserialization.
     * @return Returns [MidiSpecification] object deserialized from the string.
     */
    override fun deserialize(decoder: Decoder): MidiSpecification = when (val string = decoder.decodeString()) {
        "GM" -> MidiSpecification.GeneralMidi
        "GS" -> MidiSpecification.GeneralStandardMidi
        "XG" -> MidiSpecification.ExtendedGeneralMidi
        else -> throw IllegalArgumentException("Unknown MIDI specification: $string")
    }
}

