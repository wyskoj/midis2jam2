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

@file:UseSerializers(FileAsStringSerializer::class)

package org.wysko.midis2jam2.starter.configuration

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.wysko.midis2jam2.util.FileAsStringSerializer
import java.io.File
import javax.sound.midi.MidiSystem
import javax.sound.midi.Soundbank

val SOUNDBANK_FILE_EXTENSIONS = listOf("sf2", "dls")


private val CONFIG_FILE = File(APPLICATION_CONFIG_HOME, "soundbanks.json")

@Serializable
data class SoundbankConfiguration(
    val soundbanks: Set<File> = emptySet()
) : Configuration {
    /**
     * Fetches the soundbanks by iterating over the stored file paths and loading them using [MidiSystem].
     * Returns a list of [Soundbank] objects obtained from successful loading.
     *
     * @return The list of loaded [Soundbank] objects.
     */
    fun fetchSoundbanks(): List<Soundbank> = soundbanks.mapNotNull {
        try {
            MidiSystem.getSoundbank(it)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        val preserver by lazy { ConfigurationPreserver(serializer(), serializer(), CONFIG_FILE) }

        fun isValidSoundbankFile(file: File): Boolean =
            file.exists() && file.extension in SOUNDBANK_FILE_EXTENSIONS && try {
                MidiSystem.getSoundbank(file)
                true
            } catch (e: Exception) {
                false
            }
    }
}