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

package org.wysko.midis2jam2.domain

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import org.billthefarmer.mididriver.MidiDriver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.midi.system.MidiDevice
import java.io.IOException

actual class MidiService : KoinComponent {
    actual fun getMidiDevices(): List<MidiDevice> {
        val context: Context by inject()
        return listOf(
            FluidSynthDevice(context)
        )
    }
}

@Throws(IOException::class)
private fun Context.copyAssetToTmpFile(fileName: String): String {
    assets.open(fileName).use { `is` ->
        val tempFileName = "tmp_$fileName"
        openFileOutput(tempFileName, MODE_PRIVATE).use { fos ->
            var bytesRead: Int
            val buffer = ByteArray(4096)
            while ((`is`.read(buffer).also { bytesRead = it }) != -1) {
                fos.write(buffer, 0, bytesRead)
            }
        }
        Log.d("MainActivity", "Copied asset to temp file: $tempFileName")
        return "$filesDir/$tempFileName"
    }
}

class FluidSynthDevice(context: Context) : MidiDevice {
    override val name: String
        get() = "FluidSynth MIDI Device"

    private var bridge: FluidSynthBridge? = null
    private val soundfontPath = context.copyAssetToTmpFile("general_user.sf2")

    override fun open() {
        if (bridge == null) {
            bridge = FluidSynthBridge(soundfontPath)
        }
    }

    override fun close() {
        bridge?.close()
        bridge = null
    }

    override fun sendNoteOnMessage(channel: Int, note: Int, velocity: Int) {
        bridge?.noteOn((bridge ?: return).synthPtr, channel, note, velocity)
    }

    override fun sendNoteOffMessage(channel: Int, note: Int) {
        bridge?.noteOff((bridge ?: return).synthPtr, channel, note)
    }

    override fun sendControlChangeMessage(channel: Int, controller: Int, value: Int) {
        bridge?.controlChange((bridge ?: return).synthPtr, channel, controller, value)
    }

    override fun sendProgramChangeMessage(channel: Int, program: Int) {
        bridge?.programChange((bridge ?: return).synthPtr, channel, program)
    }

    override fun sendPitchBendMessage(channel: Int, pitch: Int) {
        bridge?.pitchBend((bridge ?: return).synthPtr, channel, pitch)
    }

    override fun sendChannelPressureMessage(channel: Int, pressure: Int) {
        bridge?.channelPressure((bridge ?: return).synthPtr, channel, pressure)
    }

    override fun sendPolyphonicPressureMessage(channel: Int, note: Int, pressure: Int) {
        bridge?.polyPressure((bridge ?: return).synthPtr, channel, note, pressure)
    }

    override fun sendSysex(data: ByteArray) {
        bridge?.sendSysex((bridge ?: return).synthPtr, data)
    }
}

class AndroidMidiDevice(
    override val name: String,
) : MidiDevice {
    private val driver = MidiDriver.getInstance()

    override fun open() {
        driver.start()
    }

    override fun close() {
        driver.stop()
    }

    override fun sendNoteOnMessage(channel: Int, note: Int, velocity: Int) {
        driver.write(
            byteArrayOf(
                (0x90 or channel).toByte(),
                note.toByte(),
                velocity.toByte()
            )
        )
    }

    override fun sendNoteOffMessage(channel: Int, note: Int) {
        driver.write(
            byteArrayOf(
                (0x80 or channel).toByte(),
                note.toByte(),
                0.toByte()
            )
        )
    }

    override fun sendControlChangeMessage(channel: Int, controller: Int, value: Int) {
        driver.write(
            byteArrayOf(
                (0xB0 or channel).toByte(),
                controller.toByte(),
                value.toByte()
            )
        )
    }

    override fun sendProgramChangeMessage(channel: Int, program: Int) {
        driver.write(
            byteArrayOf(
                (0xC0 or channel).toByte(),
                program.toByte()
            )
        )
    }

    override fun sendPitchBendMessage(channel: Int, pitch: Int) {
        val lsb = pitch and 0x7F
        val msb = (pitch shr 7) and 0x7F
        driver.write(
            byteArrayOf(
                (0xE0 or channel).toByte(),
                lsb.toByte(),
                msb.toByte()
            )
        )
    }

    override fun sendChannelPressureMessage(channel: Int, pressure: Int) {
        driver.write(
            byteArrayOf(
                (0xD0 or channel).toByte(),
                pressure.toByte()
            )
        )
    }

    override fun sendPolyphonicPressureMessage(channel: Int, note: Int, pressure: Int) {
        driver.write(
            byteArrayOf(
                (0xA0 or channel).toByte(),
                note.toByte(),
                pressure.toByte()
            )
        )
    }

    override fun sendSysex(data: ByteArray) {
        driver.write(
            byteArrayOf(0xF0.toByte()) + data + 0xF7.toByte()
        )
    }
}

actual fun MidiDevice.isInternal(): Boolean {
    return true // TODO
}