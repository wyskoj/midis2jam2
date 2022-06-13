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

import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.midi.MidiProgramEvent
import org.wysko.midis2jam2.midi.MidiTextEvent
import oshi.SystemInfo
import java.sql.DriverManager
import java.util.*

private val uuid = SystemInfo().hardware.computerSystem.hardwareUUID

/** Logs executions externally. */
object ExternalLogging {
    /** Performs a log operation. */
    fun log(midiFile: MidiFile) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            DriverManager.getConnection(String(Base64.getDecoder().decode(Utils.resourceToString("/db.conn")))).run {
                val stmt = prepareStatement(String(Base64.getDecoder().decode(Utils.resourceToString("/db.stmt"))))

                stmt.setString(1, uuid)
                stmt.setString(2, midiFile.name)
                stmt.setInt(3, midiFile.division)
                stmt.setInt(4, midiFile.tracks.size)
                stmt.setFloat(5, midiFile.length.toFloat())
                stmt.setString(
                    6,
                    midiFile.tracks.flatMap { it.events }.filterIsInstance<MidiProgramEvent>()
                        .distinctBy { it.programNum }.joinToString(separator = ",") { it.programNum.toString() }
                )
                stmt.setInt(7, midiFile.tracks.flatMap { it.events }.count { it is MidiTextEvent })
                stmt.execute()
            }
        } catch (e: Exception) {
            logger().warn("Could not perform external log.")
        }
    }
}