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

@file:UseSerializers(FileAsStringSerializer::class)

package org.wysko.midis2jam2.starter.configuration

import kotlinx.serialization.UseSerializers
import org.wysko.midis2jam2.util.FileAsStringSerializer
import kotlinx.serialization.Serializable
import java.io.File

private val CONFIG_FILE = File(APPLICATION_CONFIG_HOME, "playlist.json")

@Serializable
data class PlaylistConfiguration(
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val playlist: List<File> = listOf()
) : Configuration {
    companion object {
        val preserver by lazy {
            ConfigurationPreserver(
                serializer(),
                serializer(), CONFIG_FILE
            )
        }
    }
}