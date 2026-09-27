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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelState
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelStateTimeline
import org.wysko.midis2jam2.instrument.algorithmic.assignment.KitLayout
import org.wysko.midis2jam2.instrument.algorithmic.assignment.PRIMARY_RHYTHM_CHANNEL
import org.wysko.midis2jam2.instrument.algorithmic.assignment.VoiceResolver
import org.wysko.midis2jam2.instrument.family.percussion.AuxiliaryPercussion
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.logger
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Assigns instruments to MIDI data.
 */
object InstrumentAssignment {
    /**
     * Given a [midiFile], determines the appropriate instruments to properly visualize the events within.
     */
    fun assign(
        context: PerformanceManager,
        midiFile: TimeBasedSequence,
        onLoadingProgress: (Float) -> Unit = {},
    ): List<Instrument> {
        // Begin by extracting events from tracks and assign them to their target channels.
        val channels =
            with(midiFile.smf.tracks.flatMap { it.events }.filterIsInstance<MidiEvent>()) {
                Array(16) { arr -> this.filter { it.channel.toInt() == arr }.toMutableList() }
            }

        // As a safety precaution, we will sort each channel by the time of each event.
        channels.onEach { channel -> channel.sortBy { it.tick } }

        // Channel 10 isn't necessarily the only rhythm channel, nor always one.
        val timeline = ChannelStateTimeline.from(midiFile.smf.tracks.flatMap { it.events })

        // Create a place for instruments to go.
        val instruments = mutableListOf<Instrument>()

        // Notes on rhythm channels are pooled across channels, and turned into drums once every channel is read.
        val rhythmNotes = mutableListOf<RhythmNote>()
        var primaryKitSpans = emptyList<KitSpan>()
        var drumInsertionIndex = 0

        // For each channel,
        channels.forEachIndexed { channel, channelSpecificEvents ->
            // Instrument created relies on the program change events.
            val programEvents = channelSpecificEvents.filterIsInstance<ProgramEvent>().toMutableList()
            if (programEvents.isEmpty()) { // If there are no program events, we default to instrument 0.
                programEvents += ProgramEvent(0, channel.toByte(), 0)
            }
            programEvents.removeDuplicateProgramEvents()

            // We create "bins" that events fall into based on their corresponding program event.
            val programBins = buildMap<Byte, MutableList<MidiEvent>> {
                programEvents.distinctBy { it.program }.forEach { this += it.program to mutableListOf() }
            }

            // Since a program change event can occur in between an ON and OFF event, we also need to keep track of the
            // current program when an ON event occurs, so that the corresponding OFF event can be assigned to the same
            // instrument.
            val programPerNote = mutableMapOf<Byte, Byte>()

            // Likewise, an OFF event goes wherever its ON event went, even if the channel changed state in between.
            val rhythmKitPerNote = mutableMapOf<Byte, Int>()

            channelSpecificEvents.forEach { event ->
                if (event !is NoteEvent.NoteOff) { // If the event is not an OFF event,
                    // Determine the last program event
                    val currentProgram = programEvents.lastOrNull { it.tick <= event.tick }?.program ?: 0

                    if (event is NoteEvent.NoteOn && timeline.stateAt(channel, event.tick) == ChannelState.Rhythm) {
                        val kit = timeline.kitAt(channel, event.tick, currentProgram.toInt())
                        rhythmNotes += RhythmNote(channel, kit, event)
                        rhythmKitPerNote[event.note] = kit
                    } else {
                        // Add the event to the correct bin
                        programBins[currentProgram]?.plusAssign(event)
                        if (event is NoteEvent.NoteOn) rhythmKitPerNote.remove(event.note)
                    }

                    // Keep track of current program for ON events.
                    if (event is NoteEvent.NoteOn) programPerNote[event.note] = currentProgram
                } else {
                    rhythmKitPerNote[event.note]?.let { rhythmNotes += RhythmNote(channel, it, event) }
                        ?: programBins[programPerNote[event.note]]?.plusAssign(event)
                        ?: kotlin.run { logger().warn("Unbalanced MIDI note events.") }
                }
            }

            if (channel == PRIMARY_RHYTHM_CHANNEL) {
                drumInsertionIndex = instruments.size
                primaryKitSpans = kitSpans(programEvents, rhythmNotes.filter { it.channel == channel })
            }

            // Convert lists of events to their corresponding instrument.
            programBins.entries.forEachIndexed { i, e ->
                onLoadingProgress((channel / 16f) + (i / programBins.entries.size / 16f))
                buildInstrument(context, e.key, e.value, channelSpecificEvents)?.let { instruments += it }
            }
        }

        // The drums go where channel 10's would always have gone, so the order of the band doesn't change.
        instruments.addAll(drumInsertionIndex, buildDrums(context, rhythmNotes, timeline, primaryKitSpans))

        return instruments
    }

    /**
     * Builds the drum sets, special cases and auxiliary percussion for every rhythm note in the file.
     *
     * Only one drum set is on stage at a time, so a rhythm channel other than channel 10 plays its drum set notes on
     * whatever kit channel 10 is using, as long as both kits lay out their notes the same way. Everything else about
     * a note (special cases, auxiliary percussion) follows the note's own kit.
     */
    private fun buildDrums(
        context: PerformanceManager,
        rhythmNotes: List<RhythmNote>,
        timeline: ChannelStateTimeline,
        primaryKitSpans: List<KitSpan>,
    ): List<Instrument> {
        val drumSetBins = rhythmNotes.groupBy(
            keySelector = { note ->
                if (note.channel == PRIMARY_RHYTHM_CHANNEL || note.event !is NoteEvent.NoteOn) return@groupBy note.kit
                val primaryKit = timeline.primaryKitAt(note.event.tick, primaryKitSpans) ?: return@groupBy note.kit
                if (canFold(note.kit, primaryKit, note.event.note.toInt())) primaryKit else note.kit
            },
            valueTransform = { it.event },
        )
        val kitBins = rhythmNotes.groupBy(keySelector = { it.kit }, valueTransform = { it.event })

        val auxiliary = mutableMapOf<KClass<out AuxiliaryPercussion>, MutableList<MutableList<MidiEvent>>>()

        return buildList {
            drumSetBins.forEach { (kit, events) ->
                VoiceResolver.kit(kit).buildDrumSet(context, events.sortedBy { it.tick })?.let { add(it) }
            }
            kitBins.forEach { (kit, events) ->
                val look = VoiceResolver.kit(kit)
                val sorted = events.sortedBy { it.tick }
                addAll(look.buildSpecialCases(context, sorted))
                look.collectAuxiliary(sorted).forEach { (t, u) ->
                    if (auxiliary[t] == null) {
                        auxiliary[t] = u.map { it.toMutableList() }.toMutableList()
                    } else {
                        u.forEachIndexed { index, list ->
                            auxiliary[t]!![index].addAll(list)
                            auxiliary[t]!![index] = auxiliary[t]!![index].sortedBy { it.tick }.toMutableList()
                        }
                    }
                }
            }

            // Add auxiliary percussion
            addAll(
                auxiliary.map { (k, v) ->
                    k.primaryConstructor?.call(context, *v.toTypedArray()) ?: error("Invalid auxiliary percussion")
                }
            )
        }
    }

    /**
     * The stretches of channel 10 between program changes that actually have rhythm notes in them.
     *
     * An idle channel 10 still has a kit (Standard, by default), but it shouldn't pull another channel's notes onto
     * a drum set nobody is playing.
     */
    private fun kitSpans(programEvents: List<ProgramEvent>, notes: List<RhythmNote>): List<KitSpan> =
        programEvents.mapIndexedNotNull { i, programEvent ->
            val end = programEvents.getOrNull(i + 1)?.tick ?: Int.MAX_VALUE
            KitSpan(programEvent.tick, end, programEvent.program.toInt()).takeIf { span ->
                notes.any { it.event is NoteEvent.NoteOn && it.event.tick in span.start until span.end }
            }
        }

    /** The kit channel 10 is playing at [tick], or `null` if it isn't playing drums then. */
    private fun ChannelStateTimeline.primaryKitAt(tick: Int, spans: List<KitSpan>): Int? {
        if (stateAt(PRIMARY_RHYTHM_CHANNEL, tick) != ChannelState.Rhythm) return null
        val span = spans.firstOrNull { tick in it.start until it.end } ?: return null
        return kitAt(PRIMARY_RHYTHM_CHANNEL, tick, span.program)
    }

    /**
     * Whether [note] from kit [from] can be played on [onto]'s drum set.
     *
     * Most kits share the General MIDI drum layout and differ only in sound, but the orchestra and SFX kits have their
     * own layouts, and a note either kit turns into a special case isn't a drum set note at all.
     */
    private fun canFold(from: Int, onto: Int, note: Int): Boolean {
        val fromLook = VoiceResolver.kit(from)
        val ontoLook = VoiceResolver.kit(onto)
        return fromLook.layout == KitLayout.General &&
            ontoLook.layout == KitLayout.General &&
            note !in fromLook.specialCaseNotes &&
            note !in ontoLook.specialCaseNotes
    }

    /** A note on a rhythm channel, and the kit it was played on. */
    private class RhythmNote(val channel: Int, val kit: Int, val event: NoteEvent)

    /** A stretch of ticks, [start] inclusive to [end] exclusive, where a channel had [program] selected. */
    private class KitSpan(val start: Int, val end: Int, val program: Int)

    private fun buildInstrument(
        context: PerformanceManager,
        program: Byte,
        events: MutableList<MidiEvent>,
        allChannelEvents: List<MidiEvent>,
    ): Instrument? {
        if (events.none { it is NoteEvent }) return null

        // We need to also get Pitch Bend RPN events because they are "sticky" meaning if the events occurred during one
        // instrument, it should apply to the next if the channel program changes. We can do this by just collecting all
        // the controller events.
        // Eliminates any duplicate events from adding the two together
        @Suppress("NAME_SHADOWING")
        val events = (events + allChannelEvents.filterIsInstance<ControlChangeEvent>()).distinct().sortedBy { it.tick }

        return VoiceResolver.melodic(program.toInt())?.build(context, events)
    }

    fun MutableList<ProgramEvent>.removeDuplicateProgramEvents() {
        // Remove program events at same time (keep the last one)
        for (i in size - 2 downTo 0) {
            while (i < size - 1 && this[i].tick == this[i + 1].tick) {
                removeAt(i)
            }
        }

        // Remove program events with same value (keep the first one)
        for (i in size - 2 downTo 0) {
            while (i != size - 1 && this[i].program == this[i + 1].program) {
                removeAt(i + 1)
            }
        }
    }
}
