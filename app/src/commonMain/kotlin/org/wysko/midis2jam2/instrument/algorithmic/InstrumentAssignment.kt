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
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelSetup
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelSetupTimeline
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelState
import org.wysko.midis2jam2.instrument.algorithmic.assignment.KitLayout
import org.wysko.midis2jam2.instrument.algorithmic.assignment.KitLook
import org.wysko.midis2jam2.instrument.algorithmic.assignment.Look
import org.wysko.midis2jam2.instrument.algorithmic.assignment.MidiMode
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

        // What each channel is set up to play (bank, program, melody or rhythm), and when that changes.
        val timeline = ChannelSetupTimeline.from(midiFile.smf.tracks.flatMap { it.events }, initialMode(context))
        val resolver = VoiceResolver()

        // Create a place for instruments to go.
        val instruments = mutableListOf<Instrument>()

        // Notes on rhythm channels are pooled across channels, and turned into drums once every channel is read.
        val rhythmNotes = mutableListOf<RhythmNote>()
        var primaryKitSpans = emptyList<KitSpan>()
        var drumInsertionIndex = 0

        // For each channel,
        channels.forEachIndexed { channel, channelSpecificEvents ->
            val programEvents = channelSpecificEvents.filterIsInstance<ProgramEvent>()

            // A channel that never changes program plays program 0. One that does plays nothing melodic before its
            // first program change.
            fun setupAt(tick: Int): ChannelSetup = timeline.setupAt(channel, tick).let {
                if (it.program == null && programEvents.isEmpty()) it.copy(program = 0) else it
            }

            // Each voice appears as a look, and events fall into a "bin" for each look. Voices that look the same
            // share a bin, and so share an instrument. The bins are made in program change order, so the instruments
            // come out in that order too.
            val lookBins = LinkedHashMap<Look, MutableList<MidiEvent>>()
            programEvents.map { it.tick }.ifEmpty { listOf(0) }.forEach { tick ->
                resolver.melodic(setupAt(tick))?.let { lookBins.getOrPut(it) { mutableListOf() } }
            }

            // Since a program change event can occur in between an ON and OFF event, we also need to keep track of the
            // look when an ON event occurs, so that the corresponding OFF event can be assigned to the same instrument.
            val lookPerNote = mutableMapOf<Byte, Look?>()

            // Likewise, an OFF event goes wherever its ON event went, even if the channel changed state in between.
            val rhythmKitPerNote = mutableMapOf<Byte, KitLook>()

            channelSpecificEvents.forEach { event ->
                if (event !is NoteEvent.NoteOff) { // If the event is not an OFF event,
                    val setup = setupAt(event.tick)

                    if (event is NoteEvent.NoteOn && setup.state == ChannelState.Rhythm) {
                        val kit = resolver.kit(setup)
                        rhythmNotes += RhythmNote(channel, kit, event)
                        rhythmKitPerNote[event.note] = kit
                    } else {
                        // Add the event to the correct bin
                        val look = setup.program?.let { resolver.melodic(setup) }
                        if (look != null) lookBins.getOrPut(look) { mutableListOf() } += event
                        if (event is NoteEvent.NoteOn) {
                            rhythmKitPerNote.remove(event.note)
                            if (setup.program != null) lookPerNote[event.note] = look
                        }
                    }
                } else {
                    val kit = rhythmKitPerNote[event.note]
                    when {
                        kit != null -> rhythmNotes += RhythmNote(channel, kit, event)
                        event.note in lookPerNote -> lookPerNote[event.note]?.let { lookBins.getValue(it) += event }
                        else -> logger().warn("Unbalanced MIDI note events.")
                    }
                }
            }

            if (channel == PRIMARY_RHYTHM_CHANNEL) {
                drumInsertionIndex = instruments.size
                primaryKitSpans = kitSpans(
                    programEvents.map { it.tick }.ifEmpty { listOf(0) },
                    rhythmNotes.filter { it.channel == channel },
                )
            }

            // Convert lists of events to their corresponding instrument.
            lookBins.entries.forEachIndexed { i, e ->
                onLoadingProgress((channel / 16f) + (i / lookBins.entries.size / 16f))
                buildInstrument(context, e.key, e.value, channelSpecificEvents)?.let { instruments += it }
            }
        }

        // The drums go where channel 10's would always have gone, so the order of the band doesn't change.
        instruments.addAll(drumInsertionIndex, buildDrums(context, rhythmNotes, timeline, resolver, primaryKitSpans))

        return instruments
    }

    /**
     * The specification the synthesizer is in before the file sends a reset of its own: the one the app sends a reset
     * message for, if it's set to.
     */
    private fun initialMode(context: PerformanceManager): MidiMode {
        val reset = context.config.settings.playbackSettings.midiSpecificationResetSettings
        if (!reset.isSendSpecificationResetMessage) return MidiMode.GM
        return when (reset.midiSpecification) {
            MidiSpecification.GeneralMidi -> MidiMode.GM
            MidiSpecification.ExtendedGeneral -> MidiMode.XG
            MidiSpecification.GeneralStandard -> MidiMode.GS
        }
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
        timeline: ChannelSetupTimeline,
        resolver: VoiceResolver,
        primaryKitSpans: List<KitSpan>,
    ): List<Instrument> {
        val drumSetBins = rhythmNotes.groupBy(
            keySelector = { note ->
                if (note.channel == PRIMARY_RHYTHM_CHANNEL || note.event !is NoteEvent.NoteOn) return@groupBy note.kit
                val primaryKit = timeline.primaryKitAt(note.event.tick, primaryKitSpans, resolver)
                    ?: return@groupBy note.kit
                if (canFold(note.kit, primaryKit, note.event.note.toInt())) primaryKit else note.kit
            },
            valueTransform = { it.event },
        )
        val kitBins = rhythmNotes.groupBy(keySelector = { it.kit }, valueTransform = { it.event })

        val auxiliary = mutableMapOf<KClass<out AuxiliaryPercussion>, MutableList<MutableList<MidiEvent>>>()

        return buildList {
            drumSetBins.forEach { (kit, events) ->
                kit.buildDrumSet(context, events.sortedBy { it.tick })?.let { add(it) }
            }
            kitBins.forEach { (kit, events) ->
                val sorted = events.sortedBy { it.tick }
                addAll(kit.buildSpecialCases(context, sorted))
                kit.collectAuxiliary(sorted).forEach { (t, u) ->
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
    private fun kitSpans(programChangeTicks: List<Int>, notes: List<RhythmNote>): List<KitSpan> =
        programChangeTicks.mapIndexedNotNull { i, start ->
            val end = programChangeTicks.getOrNull(i + 1) ?: Int.MAX_VALUE
            KitSpan(start, end).takeIf { span ->
                notes.any { it.event is NoteEvent.NoteOn && it.event.tick in span.start until span.end }
            }
        }

    /** The kit channel 10 is playing at [tick], or `null` if it isn't playing drums then. */
    private fun ChannelSetupTimeline.primaryKitAt(tick: Int, spans: List<KitSpan>, resolver: VoiceResolver): KitLook? {
        val setup = setupAt(PRIMARY_RHYTHM_CHANNEL, tick)
        if (setup.state != ChannelState.Rhythm) return null
        if (spans.none { tick in it.start until it.end }) return null
        return resolver.kit(setup)
    }

    /**
     * Whether [note] from kit [from] can be played on [onto]'s drum set.
     *
     * Most kits share the General MIDI drum layout and differ only in sound, but the orchestra and SFX kits have their
     * own layouts, and a note either kit turns into a special case isn't a drum set note at all.
     */
    private fun canFold(from: KitLook, onto: KitLook, note: Int): Boolean =
        from.layout == KitLayout.General &&
            onto.layout == KitLayout.General &&
            note !in from.specialCaseNotes &&
            note !in onto.specialCaseNotes

    /** A note on a rhythm channel, and the kit it was played on. */
    private class RhythmNote(val channel: Int, val kit: KitLook, val event: NoteEvent)

    /** A stretch of ticks, [start] inclusive to [end] exclusive, between two of a channel's program changes. */
    private class KitSpan(val start: Int, val end: Int)

    private fun buildInstrument(
        context: PerformanceManager,
        look: Look,
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

        return look.build(context, events)
    }
}
