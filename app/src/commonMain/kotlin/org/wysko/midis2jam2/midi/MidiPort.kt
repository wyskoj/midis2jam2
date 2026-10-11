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

package org.wysko.midis2jam2.midi

import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.SysexEvent

/** The channels each port has. */
const val CHANNELS_PER_PORT: Int = 16

/**
 * The most ports a file is played on. 16 ports is 256 channels, as many as FluidSynth can have. A file that uses more
 * shares them out: its 17th port plays on the 1st, and so on.
 */
const val MAX_PORTS: Int = 16

/**
 * An [event], and the [port] it is sent to.
 *
 * The port is an index from 0 to [portCount] - 1, not the number the file gives it: the ports a file uses are numbered
 * in order, so a file on ports 0 and 3 is played on ports 0 and 1.
 */
data class PortedEvent(val port: Int, val event: Event)

/**
 * A single number for [channel] on [port], unique across the file: port 0's channels are 0 to 15, port 1's 16 to 31,
 * and so on. A file without MIDI Port events only uses port 0, so its channel keys are its channels.
 */
fun channelKey(port: Int, channel: Int): Int = port * CHANNELS_PER_PORT + channel

/** The port of a [channel key][channelKey]. */
fun portOf(channelKey: Int): Int = channelKey / CHANNELS_PER_PORT

/** The channel, within its port, of a [channel key][channelKey]. */
fun channelOf(channelKey: Int): Int = channelKey % CHANNELS_PER_PORT

/**
 * The number of ports this file is played on: one for each port it sends channel or system exclusive messages to, up
 * to [MAX_PORTS]. At least one, even for a file with no messages at all.
 */
val StandardMidiFile.portCount: Int
    get() = portIndices().size.coerceIn(1, MAX_PORTS)

/**
 * Every event in this file with the port it is sent to, in the order it is played: by tick, and events on the same
 * tick in track order, then in the order they appear in their track (the same order as [eventsInPlayOrder]).
 *
 * A track's events are sent to port 0 until it has a [MIDI Port][MetaEvent.MidiPort] event, and to that event's port
 * from then on.
 */
fun StandardMidiFile.portedEventsInPlayOrder(): List<PortedEvent> {
    val indices = portIndices()
    return tracks.flatMap { track ->
        track.eventsWithFilePorts().map { (port, event) -> PortedEvent(indices[port]?.rem(MAX_PORTS) ?: 0, event) }
    }.sortedBy { it.event.tick }
}

/** The index of each port, as the file numbers it, that the file sends a message to. */
private fun StandardMidiFile.portIndices(): Map<Int, Int> = tracks
    .flatMap { track ->
        track.eventsWithFilePorts().filter { (_, event) -> event is MidiEvent || event is SysexEvent }.map { it.first }
    }
    .distinct()
    .sorted()
    .withIndex()
    .associate { (index, port) -> port to index }

/** Each event in this track, with the port it is sent to as the file numbers it. */
internal fun StandardMidiFile.Track.eventsWithFilePorts(): List<Pair<Int, Event>> {
    var port = 0
    return events.map { event ->
        if (event is MetaEvent.MidiPort) port = event.port.toInt()
        port to event
    }
}
