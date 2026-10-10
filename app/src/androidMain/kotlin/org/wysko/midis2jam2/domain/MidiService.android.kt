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
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context.MODE_PRIVATE
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.midi.system.MidiDevice
import java.io.File
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

/**
 * Copies a bundled asset out to a file, since FluidSynth can only load a soundbank from a path, and returns its path.
 *
 * The copy is kept between performances, and only made again once the app has been installed or updated since, so
 * starting a performance doesn't spend its loading time copying a soundbank that hasn't changed. It's written beside
 * the copy and then renamed over it, so that a copy cut short never passes for a complete one.
 */
@Throws(IOException::class)
private fun Context.copyAssetToFile(fileName: String): String {
    val file = File(filesDir, "tmp_$fileName")
    if (file.exists() && file.lastModified() >= appLastUpdateTime()) return file.absolutePath

    val partial = File(filesDir, "tmp_$fileName.partial")
    assets.open(fileName).use { input -> partial.outputStream().use { input.copyTo(it) } }
    if (!partial.renameTo(file)) throw IOException("Could not move the copy of $fileName into place")
    Log.d("MidiService", "Copied asset to file: ${file.name}")
    return file.absolutePath
}

/** When this app was last installed or updated. */
private fun Context.appLastUpdateTime(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).lastUpdateTime
    }

@Throws(IOException::class)
internal fun Context.copyBytesToInternalStorage(fileName: String, bytes: ByteArray): String {
    val dir = getDir("soundbanks", MODE_PRIVATE)
    val safeFileName = java.io.File(fileName).name
    if (safeFileName.isBlank()) {
        throw IOException("Invalid file name")
    }
    val destFile = java.io.File(dir, safeFileName)
    destFile.writeBytes(bytes)
    Log.d("MidiService", "Copied soundbank to internal storage: ${destFile.absolutePath}")
    return destFile.absolutePath
}

class FluidSynthDevice(context: Context) : MidiDevice {
    override val name: String
        get() = "FluidSynth MIDI Device"

    private var bridge: FluidSynthBridge? = null
    private val builtInSoundfontPath = context.copyAssetToFile("general_user.sf2")

    /** Override path to a user-supplied SF2 file. Set before calling [open]. */
    var soundfontOverridePath: String? = null

    override fun open() {
        if (bridge == null) {
            val path = soundfontOverridePath
                ?.takeIf { java.io.File(it).exists() }
                ?: builtInSoundfontPath
            bridge = FluidSynthBridge(path)
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
