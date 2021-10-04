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

class LauncherSettings {
    var lastMidiDir: String = JFileChooser().fileSystemView.defaultDirectory.absolutePath
    var soundFontPaths: List<String> = ArrayList()
    var transition: InstrumentTransition = InstrumentTransition.NORMAL
    var midiDevice: String = "Gervill"
    var isFullscreen: Boolean = false
    private val deviceLatencyMap: MutableMap<String, Int> = HashMap<String, Int>().also {
        it["Gervill"] = 100
    }
    var isLegacyDisplay: Boolean = false
    var locale: String = "en"

    fun getLatencyForDevice(deviceName: String) = deviceLatencyMap[deviceName] ?: 0

    fun setLatencyForDevice(deviceName: String, value: Int) {
        deviceLatencyMap[deviceName] = value
    }
}