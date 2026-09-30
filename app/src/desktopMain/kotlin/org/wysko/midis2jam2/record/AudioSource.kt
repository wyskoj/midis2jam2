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

import kotlin.time.Duration

/**
 * A stream of interleaved 16-bit PCM audio to put in a recording.
 */
interface AudioSource : AutoCloseable {
    val sampleRate: Int
    val channels: Int

    /**
     * Reads up to [count] interleaved samples into the start of [buffer], returning how many were read, or `-1` at the
     * end of the stream. [count] is a whole number of frames (a multiple of [channels]), and so is the result.
     */
    fun read(buffer: ShortArray, count: Int): Int

    override fun close() = Unit
}

/**
 * Returns an [AudioSource] that plays [duration] of silence before this one.
 */
fun AudioSource.withLeadingSilence(duration: Duration): AudioSource {
    val source = this
    return object : AudioSource by source {
        private var silentSamplesLeft =
            (duration.inWholeMicroseconds * source.sampleRate / 1_000_000).coerceAtLeast(0) * source.channels

        override fun read(buffer: ShortArray, count: Int): Int {
            if (silentSamplesLeft == 0L) return source.read(buffer, count)
            val silent = minOf(count.toLong(), silentSamplesLeft).toInt()
            buffer.fill(0, 0, silent)
            silentSamplesLeft -= silent
            return silent
        }
    }
}
