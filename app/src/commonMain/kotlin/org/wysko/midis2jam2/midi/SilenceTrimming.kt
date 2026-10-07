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
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.event.ChannelPressureEvent
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.PitchWheelChangeEvent
import org.wysko.kmidi.midi.event.PolyphonicKeyPressureEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent
import org.wysko.midis2jam2.domain.settings.AppSettings

/**
 * Reads this file as the sequence a performance plays, trimming its silence first if [settings] say to.
 *
 * @see trimSilence
 */
fun StandardMidiFile.toPerformanceSequence(settings: AppSettings): TimeBasedSequence =
    (if (settings.playbackSettings.isTrimSilence) trimSilence() else this).toTimeBasedSequence()

/**
 * Every event in this file, in the order it is played: by tick, and events on the same tick in track order, then in
 * the order they appear in their track.
 */
fun StandardMidiFile.eventsInPlayOrder(): List<Event> = tracks.flatMap { it.events }.sortedBy { it.tick }

/**
 * Removes the silence before the first note and after the last one.
 *
 * The first note is moved to tick 0, and everything after it keeps its spacing. Whatever came before the first note
 * (resets, bank and program changes, controllers, tempo) is still sent: all of it is moved to tick 0, at the front of
 * the first track, in the order it would have been played. Every part of the app reads a file in
 * [play order][eventsInPlayOrder], so that run of events comes out first and in its original order, and is sent all
 * at once just before the first note. The last of any tempo changes in it is the one in effect at the first note, so
 * the timing of the song is unchanged.
 *
 * Track names and other events that belong to a particular track stay in that track. Events after the last note are
 * dropped: once the notes are over there is nothing left to hear, and the sequencer silences every note when it
 * stops.
 *
 * A file with no notes, or no silence to trim, is returned as it is.
 */
fun StandardMidiFile.trimSilence(): StandardMidiFile {
    val allEvents = tracks.flatMap { it.events }
    val start = allEvents.filterIsInstance<NoteEvent.NoteOn>().minOfOrNull { it.tick } ?: return this
    val end = allEvents.filterIsInstance<NoteEvent>().maxOf { it.tick }

    if (start == 0 && allEvents.none { it.tick > end }) return this

    val preRoll = eventsInPlayOrder()
        .filter { it.tick < start && !it.belongsToItsTrack() && it !is NoteEvent && it !is MetaEvent.EndOfTrack }
        .map { it.withTick(0) }

    val trimmedTracks = tracks.mapIndexed { index, track ->
        val identity = track.events.filter { it.tick < start && it.belongsToItsTrack() }
        val body = track.events
            .filter { it.tick in start..end && it !is MetaEvent.EndOfTrack }
            .map { it.withTick(it.tick - start) }
        val endOfTrack = (track.events.filterIsInstance<MetaEvent.EndOfTrack>().maxOfOrNull { it.tick } ?: end)
            .coerceIn(start, end) - start

        StandardMidiFile.Track(
            identity + (if (index == 0) preRoll else emptyList()) + body + MetaEvent.EndOfTrack(endOfTrack)
        )
    }

    return copy(tracks = trimmedTracks)
}

/** Whether this event describes the track it is in, and so has to stay there. These can only occur at tick 0. */
private fun Event.belongsToItsTrack(): Boolean = when (this) {
    is MetaEvent.SequenceTrackName,
    is MetaEvent.InstrumentName,
    is MetaEvent.CopyrightNotice,
    is MetaEvent.SequenceNumber,
    is MetaEvent.SmpteOffset,
    -> true

    else -> false
}

/** This event, moved to [tick]. */
private fun Event.withTick(tick: Int): Event = when (this) {
    is NoteEvent.NoteOn -> copy(tick = tick)
    is NoteEvent.NoteOff -> copy(tick = tick)
    is ControlChangeEvent -> copy(tick = tick)
    is ProgramEvent -> copy(tick = tick)
    is PitchWheelChangeEvent -> copy(tick = tick)
    is ChannelPressureEvent -> copy(tick = tick)
    is PolyphonicKeyPressureEvent -> copy(tick = tick)
    is SysexEvent -> copy(tick = tick)
    is MetaEvent.Text -> copy(tick = tick)
    is MetaEvent.Lyric -> copy(tick = tick)
    is MetaEvent.Marker -> copy(tick = tick)
    is MetaEvent.CuePoint -> copy(tick = tick)
    is MetaEvent.ChannelPrefix -> copy(tick = tick)
    is MetaEvent.EndOfTrack -> copy(tick = tick)
    is MetaEvent.SetTempo -> copy(tick = tick)
    is MetaEvent.TimeSignature -> copy(tick = tick)
    is MetaEvent.KeySignature -> copy(tick = tick)
    is MetaEvent.SequencerSpecific -> copy(tick = tick)
    is MetaEvent.Unknown -> copy(tick = tick)
    // The rest are always at tick 0, or are made by analysis rather than read from a file.
    else -> this
}
