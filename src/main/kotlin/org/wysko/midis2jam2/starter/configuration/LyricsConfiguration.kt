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

package org.wysko.midis2jam2.starter.configuration

import kotlinx.serialization.Serializable
import java.io.File

private val CONFIG_FILE = File(APPLICATION_CONFIG_HOME, "lyrics.json")

@Serializable
data class LyricsConfiguration(
    val lyricSize: LyricSize
) : Configuration {
    companion object {
        /**
         * The preserver for the graphics configuration.
         */
        val preserver: ConfigurationPreserver<LyricsConfiguration> by lazy {
            ConfigurationPreserver(serializer(), serializer(), CONFIG_FILE)
        }
    }
}

@Serializable
data class LyricSize(val times: Float) {
    override fun toString(): String = "x$times"

    companion object {
        val OPTIONS: List<LyricSize> = listOf(
            LyricSize(0.5f),
            LyricSize(1f),
            LyricSize(1.5f),
            LyricSize(2f),
            LyricSize(2.5f)
        )
    }
}