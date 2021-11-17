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

import org.wysko.midis2jam2.util.InstrumentTransition
import javax.swing.JFileChooser

/**
 * Defines the settings for the launcher.
 */
class LauncherSettings {
    /** The last directory used for opening files. */
    var lastMidiDir: String = JFileChooser().fileSystemView.defaultDirectory.absolutePath

    /** A list of SoundFonts to use. */
    var soundFontPaths: List<String> = ArrayList()

    /** The instrument transition to use. */
    var transition: InstrumentTransition = InstrumentTransition.NORMAL

    /** The MIDI device to use. */
    var midiDevice: String = "Gervill"

    /** Use fullscreen? */
    var isFullscreen: Boolean = false

    /** Keeps track of the latency fix for each MIDI device. */
    private val deviceLatencyMap: MutableMap<String, Int> = HashMap<String, Int>().also {
        it["Gervill"] = 100
    }

    /** Use the legacy display mode? */
    var isLegacyDisplay: Boolean = false

    /** The locale to use. */
    var locale: String = "en"

    /** Given a [deviceName], returns the latency fix for that device. */
    fun getLatencyForDevice(deviceName: String): Int = deviceLatencyMap[deviceName] ?: 0

    /** Sets the latency fix for a device. */
    fun setLatencyForDevice(deviceName: String, value: Int) {
        deviceLatencyMap[deviceName] = value
    }
}