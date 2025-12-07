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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.midi.system.MidiDevice
import java.io.IOException

actual class MidiService : KoinComponent {
    actual fun getMidiDevices(): List<MidiDevice> {
        val context: Context by inject()
        return listOf(
            FluidSynthDevice(context).also {
                FluidSynthDevice.device = it
            }
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

    override fun sendData(data: ByteArray) {
        bridge?.sendSysex((bridge ?: return).synthPtr, data)
    }

    fun setChorusActive(isChorusActive: Boolean) {
        bridge?.setChorusActive(isChorusActive)
    }

    fun setReverbActive(isReverbActive: Boolean) {
        bridge?.setReverbActive(isReverbActive)
    }

    companion object {
        lateinit var device: FluidSynthDevice
    }
}
