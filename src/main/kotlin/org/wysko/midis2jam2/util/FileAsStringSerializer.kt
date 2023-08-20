/*
 * Copyright (C) 2023 Jacob Wysko
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

package org.wysko.midis2jam2.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

/**
 * An object that serializes and deserializes [File] objects to and from their string representations.
 */
object FileAsStringSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("File", PrimitiveKind.STRING)

    /**
     * Serialize a [File] into a string.
     *
     * @param   encoder [Encoder] to aid the serialization.
     * @param   value [File] object to serialize.
     */
    override fun serialize(encoder: Encoder, value: File): Unit =
        encoder.encodeString(value.absolutePath)

    /**
     * Deserialize a string into a [File].
     *
     * @param   decoder [Decoder] to aid the deserialization.
     * @return  Returns [File] object deserialized from the string.
     */
    override fun deserialize(decoder: Decoder): File =
        File(decoder.decodeString())
}