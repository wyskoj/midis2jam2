/*
 * Copyright (C) 2022 Jacob Wysko
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

import java.io.File

/** Provides some configurable settings for running the program. */
data class PassedSettings(
    /** The amount to shift the audio by to fix A/V sync, in milliseconds. */
    val latencyFix: Int,

    /** Automatically start autocam. */
    val autoAutoCam: Boolean,

    /** True if midis2jam2 should be run in fullscreen mode. */
    val fullscreen: Boolean,

    /** Use legacy display mode. */
    val legacyDisplayEngine: Boolean,

    /** The MIDI file to play. */
    val midiFile: File,

    /** The MIDI device to use. */
    val midiDevice: String,

    /** The SoundFont to use. */
    val soundFont: File?,

    /** Sub-pixel sampling */
    val samples: Int = 4,
)