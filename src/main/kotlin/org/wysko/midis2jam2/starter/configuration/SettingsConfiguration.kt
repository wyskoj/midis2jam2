/*
 * Copyright (C) 2024 Jacob Wysko
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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

private val CONFIG_FILE = File(APPLICATION_CONFIG_HOME, "settings.json")

/**
 * Represents the configuration settings for midis2jam2 application.
 */
@Serializable
data class SettingsConfiguration(
    /**
     * Indicates whether midis2jam2 is currently in fullscreen mode.
     */
    @SerialName("fullscreen")
    val isFullscreen: Boolean = false,

    /**
     * Whether the autocam starts when the song starts.
     */
    @SerialName("auto_autocam")
    val startAutocamWithSong: Boolean = false,

    /**
     * Whether to show the HUD.
     */
    @SerialName("show_hud")
    val showHud: Boolean = true,

    /**
     * Whether to show lyrics.
     */
    @SerialName("lyrics")
    val showLyrics: Boolean = true,

    /**
     * Whether to always show instruments throughout the song.
     */
    @SerialName("never_hidden")
    val instrumentsAlwaysVisible: Boolean = true,

    /**
     * Whether the free-cam is smooth.
     */
    @SerialName("smooth_camera")
    val isCameraSmooth: Boolean = true,
) : Configuration {
    companion object {
        val preserver by lazy { ConfigurationPreserver(serializer(), serializer(), CONFIG_FILE) }
    }
}