/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.record

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.midi.eventsInPlayOrder
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.midi.system.dispatch
import java.io.File
import java.io.InputStream
import javax.sound.midi.MidiMessage
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import javax.sound.midi.SysexMessage
import javax.sound.midi.Synthesizer
import javax.sound.sampled.AudioFormat
import javax.sound.midi.MidiUnavailableException

/** The JVM flag that lets [OfflineSynthesizer] reach Gervill's offline rendering API. */
const val GERVILL_EXPORTS_FLAG: String = "--add-exports=java.desktop/com.sun.media.sound=ALL-UNNAMED"

private const val SAMPLE_RATE = 48_000
private const val CHANNELS = 2
private const val BYTES_PER_SAMPLE = 2

/**
 * Gervill, rendering to memory instead of the sound card, faster than real time.
 *
 * As a [MidiDevice] it takes messages the way live playback sends them (the specification reset and effect settings
 * from `MidiDeviceManager`), stamped at [timestampMicros]. [queue] then schedules a whole song, and reading this as an
 * [AudioSource] renders it; audio time zero is the start of the song.
 *
 * Gervill's `AudioSynthesizer.openStream` isn't exported from `java.desktop`, so the JVM must be started with
 * [GERVILL_EXPORTS_FLAG].
 */
class OfflineSynthesizer(soundbank: File?) : MidiDevice, AudioSource {
    override val name: String = "Gervill (offline)"
    override val sampleRate: Int = SAMPLE_RATE
    override val channels: Int = CHANNELS

    /** The time, in microseconds of audio, that messages sent to this device take effect. */
    var timestampMicros: Long = 0

    private val synthesizer: Synthesizer = MidiSystem.getSynthesizer()
    private val stream: InputStream = openStream(synthesizer)
    private val receiver: Receiver = synthesizer.receiver
    private var byteBuffer = ByteArray(0)

    init {
        soundbank?.let { synthesizer.loadAllInstruments(MidiSystem.getSoundbank(it)) }
    }

    /**
     * Schedules every event in [sequence], after anything already sent to this device.
     */
    fun queue(sequence: TimeBasedSequence) {
        sequence.smf.eventsInPlayOrder().forEach { event ->
            timestampMicros = sequence.getTimeAtTick(event.tick).inWholeMicroseconds
            dispatch(event)
        }
    }

    override fun read(buffer: ShortArray, count: Int): Int {
        val bytes = count * BYTES_PER_SAMPLE
        if (byteBuffer.size < bytes) byteBuffer = ByteArray(bytes)
        var filled = 0
        while (filled < bytes) {
            val read = stream.read(byteBuffer, filled, bytes - filled)
            if (read < 0) break
            filled += read
        }
        val samples = filled / (BYTES_PER_SAMPLE * CHANNELS) * CHANNELS
        if (samples == 0) return -1
        for (i in 0 until samples) {
            val low = byteBuffer[i * 2].toInt() and 0xFF
            val high = byteBuffer[i * 2 + 1].toInt()
            buffer[i] = ((high shl 8) or low).toShort()
        }
        return samples
    }

    override fun open() = Unit

    override fun close() {
        runCatching { stream.close() }
        synthesizer.close()
    }

    override fun sendNoteOnMessage(channel: Int, note: Int, velocity: Int) =
        send(ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity))

    override fun sendNoteOffMessage(channel: Int, note: Int) =
        send(ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0))

    override fun sendControlChangeMessage(channel: Int, controller: Int, value: Int) =
        send(ShortMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value))

    override fun sendProgramChangeMessage(channel: Int, program: Int) =
        send(ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0))

    override fun sendPitchBendMessage(channel: Int, pitch: Int) =
        send(ShortMessage(ShortMessage.PITCH_BEND, channel, pitch and 0x7F, (pitch shr 7) and 0x7F))

    override fun sendChannelPressureMessage(channel: Int, pressure: Int) =
        send(ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, pressure, 0))

    override fun sendPolyphonicPressureMessage(channel: Int, note: Int, pressure: Int) =
        send(ShortMessage(ShortMessage.POLY_PRESSURE, channel, note, pressure))

    override fun sendData(data: ByteArray) {
        val message = if (data.first() == 0xF0.toByte()) data else byteArrayOf(0xF0.toByte()) + data
        send(SysexMessage(message, message.size))
    }

    // Gervill keeps messages with equal timestamps in the order they arrive, so the reset sent at time zero stays
    // ahead of the song's own time-zero events.
    private fun send(message: MidiMessage) = receiver.send(message, timestampMicros)

    private companion object {
        fun openStream(synthesizer: Synthesizer): InputStream {
            val format = AudioFormat(SAMPLE_RATE.toFloat(), BYTES_PER_SAMPLE * 8, CHANNELS, true, false)
            val method = try {
                Class.forName("com.sun.media.sound.AudioSynthesizer")
                    .getMethod("openStream", AudioFormat::class.java, Map::class.java)
            } catch (e: ReflectiveOperationException) {
                throw MidiUnavailableException("This Java runtime has no offline Gervill synthesizer.").apply {
                    initCause(e)
                }
            }
            return try {
                method.invoke(synthesizer, format, null) as InputStream
            } catch (e: IllegalAccessException) {
                throw MidiUnavailableException("Offline audio needs the JVM flag $GERVILL_EXPORTS_FLAG.").apply {
                    initCause(e)
                }
            }
        }
    }
}
