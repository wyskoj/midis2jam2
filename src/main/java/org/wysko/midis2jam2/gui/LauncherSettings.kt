/*
 * Copyright (C) 2021 Jacob Wysko
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

package org.wysko.midis2jam2.gui

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Defines settings that are configurable in the launcher.
 */
@Serializable
data class LauncherSettings(
    /** The last directory that the user selected a MIDI file from. Defaults to the user's home directory. */
    var lastMidiDir: String = System.getProperty("user.home"),

    /** A list of paths to SoundFonts. */
    var soundFontPaths: MutableList<String> = mutableListOf(),

    /** The currently selected MIDI device. */
    var midiDevice: String = "Gervill",

    /** True if midis2jam2 should run in fullscreen mode, false otherwise. */
    var fullscreen: Boolean = false,

    /** Stores the amount of latency fix each MIDI device should have. */
    var deviceLatencyMap: MutableMap<String, Int> = mutableMapOf("Gervill" to 0),

    /** True if the autocam should be enabled when the song starts, false otherwise. */
    var autoAutoCam: Boolean = true,

    /** True if the legacy display engine should be used, false otherwise. */
    var isLegacyDisplay: Boolean = false,

    /** The locale of the launcher. */
    var locale: String = "en"
) {
    /** Writes the settings to disk. */
    fun save() {
        launcherSettingsFile().writeText(Json.encodeToString(this))
    }
}

/** Returns the file that contains the settings for the launcher. */
private fun launcherSettingsFile(): File = File(
    "${System.getProperty("user.home")}${System.getProperty("file.separator")}midis2jam2.settings"
)

/**
 * Loads the launcher settings, stored at [launcherSettingsFile]. When loading, it also cleans out any unavailable
 * SoundFont files.
 */
internal fun loadLauncherSettingsFromFile(): LauncherSettings {
    return if (launcherSettingsFile().exists()) {
        try {
            Json.decodeFromString<LauncherSettings>(launcherSettingsFile().readText()).also { settings ->
                settings.soundFontPaths = settings.soundFontPaths.filter { File(it).exists() }.toMutableList()
            }
        } catch (e: Exception) {
            LauncherSettings()
        }
    } else {
        LauncherSettings()
    }
}