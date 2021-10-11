/*
 * Copyright (C) 2021 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.instrument.family.percussion.Triangle.TriangleType.MUTED
import org.wysko.midis2jam2.instrument.family.percussion.Triangle.TriangleType.OPEN
import org.wysko.midis2jam2.instrument.family.percussion.drumset.*
import org.wysko.midis2jam2.instrument.family.percussion.drumset.Cymbal.CymbalType.*
import org.wysko.midis2jam2.instrument.family.percussion.drumset.Tom.TomPitch.*
import org.wysko.midis2jam2.midi.Midi
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.MidiProgramEvent

class Percussion(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) : DecayedInstrument(context, events) {

    /** Contains all percussion instruments. */
    private val percussionNode: Node = Node()

    /** All note on events. */
    private val noteOnEvents: MutableList<MidiNoteOnEvent> =
        events.filterIsInstance<MidiNoteOnEvent>().filter { it.note in 27..87 } as MutableList<MidiNoteOnEvent>

    /** All program change events. */
    private val programEvents: MutableList<MidiProgramEvent> =
        events.filterIsInstance<MidiProgramEvent>() as MutableList<MidiProgramEvent>

    /** Each percussion instrument. */
    val instruments: MutableList<PercussionInstrument> = ArrayList()

    private fun eventsByNote(vararg notes: Int) = noteOnEvents.filter { notes.contains(it.note) }.toMutableList()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        instruments.forEach { it.tick(time, delta) }

        NoteQueue.collectOne(programEvents, context, time)?.let { event ->
            when (event.programNum) {
                PercussionKit.ROOM.midiNumber -> {
                    retexture("DrumShell_Snare_Room.png", "DrumShell_Room.png")
                }
                PercussionKit.BRUSH.midiNumber -> {
                    retexture("DrumShell_Snare_Brush.png", "DrumShell_Brush.png")
                }
                PercussionKit.JAZZ.midiNumber -> {
                    retexture("DrumShell_Snare_Jazz.png", "DrumShell_Jazz.png")
                }
                PercussionKit.POWER.midiNumber -> {
                    retexture("DrumShell_Snare_Power.png", "DrumShell_Power.png")
                }
                else -> {
                    retexture("DrumShell_Snare.bmp", "DrumShell.bmp")
                }
            }
        }
    }

    /** Retextures [Retexturables][Retexturable] given textures for [RetextureTypes][RetextureType]. */
    private fun retexture(snareTexture: String, otherTexture: String) =
        instruments.filterIsInstance<Retexturable>().forEach {
            if (it.retextureType() == RetextureType.SNARE) {
                it.drum().setMaterial(context.unshadedMaterial(snareTexture))
            } else if (it.retextureType() == RetextureType.OTHER) {
                it.drum().setMaterial(context.unshadedMaterial(otherTexture))
            }
        }


    override fun moveForMultiChannel(delta: Float) {
        // Do nothing!
    }

    init {
        val drumSetNode = Node()

        instruments.add(SnareDrum(context, eventsByNote(Midi.ACOUSTIC_SNARE, Midi.ELECTRIC_SNARE, Midi.SIDE_STICK)))
        instruments.add(BassDrum(context, eventsByNote(Midi.ELECTRIC_BASS_DRUM, Midi.ACOUSTIC_BASS_DRUM)))
        instruments.add(Tom(context, eventsByNote(Midi.LOW_FLOOR_TOM), LOW_FLOOR))
        instruments.add(Tom(context, eventsByNote(Midi.HIGH_FLOOR_TOM), HIGH_FLOOR))
        instruments.add(Tom(context, eventsByNote(Midi.LOW_TOM), LOW))
        instruments.add(Tom(context, eventsByNote(Midi.LOW_MID_TOM), LOW_MID))
        instruments.add(Tom(context, eventsByNote(Midi.HI_MID_TOM), HIGH_MID))
        instruments.add(Tom(context, eventsByNote(Midi.HIGH_TOM), HIGH))
        instruments.add(Cymbal(context, eventsByNote(Midi.CRASH_CYMBAL_1), CRASH_1))
        instruments.add(Cymbal(context, eventsByNote(Midi.CRASH_CYMBAL_2), CRASH_2))
        instruments.add(Cymbal(context, eventsByNote(Midi.SPLASH_CYMBAL), SPLASH))
        instruments.add(Cymbal(context, eventsByNote(Midi.CHINESE_CYMBAL), CHINA))

        /* Calculate ride cymbal notes */
        val rides = eventsByNote(Midi.RIDE_BELL, Midi.RIDE_CYMBAL_1, Midi.RIDE_CYMBAL_2)
        var currentRide = RIDE_1
        val ride1Notes = ArrayList<MidiNoteOnEvent>()
        val ride2Notes = ArrayList<MidiNoteOnEvent>()
        rides.forEach {
            when (it.note) {
                Midi.RIDE_CYMBAL_1 -> {
                    ride1Notes.add(it)
                    currentRide = RIDE_1
                }
                Midi.RIDE_CYMBAL_2 -> {
                    ride2Notes.add(it)
                    currentRide = RIDE_2
                }
                Midi.RIDE_BELL -> {
                    if (currentRide == RIDE_1) ride1Notes.add(it)
                    else ride2Notes.add(it)
                }
            }
        }

        instruments.add(RideCymbal(context, ride1Notes, RIDE_1))
        instruments.add(RideCymbal(context, ride2Notes, RIDE_2))
        instruments.add(HiHat(context, eventsByNote(Midi.PEDAL_HI_HAT, Midi.OPEN_HI_HAT, Midi.CLOSED_HI_HAT)))

        if (noteOnEvents.any { it.note.oneOf(Midi.SLAP) })
            instruments.add(Slap(context, eventsByNote(Midi.SLAP)))
        if (noteOnEvents.any { it.note.oneOf(Midi.LOW_CONGA, Midi.OPEN_HIGH_CONGA, Midi.MUTE_HIGH_CONGA) })
            instruments.add(Congas(context, eventsByNote(Midi.LOW_CONGA, Midi.OPEN_HIGH_CONGA, Midi.MUTE_HIGH_CONGA)))
        if (noteOnEvents.any { it.note.oneOf(Midi.COWBELL) })
            instruments.add(Cowbell(context, eventsByNote(Midi.COWBELL)))
        if (noteOnEvents.any { it.note.oneOf(Midi.HIGH_TIMBALE, Midi.LOW_TIMBALE) })
            instruments.add(Timbales(context, eventsByNote(Midi.HIGH_TIMBALE, Midi.LOW_TIMBALE)))
        if (noteOnEvents.any { it.note.oneOf(Midi.LOW_BONGO, Midi.HIGH_BONGO) })
            instruments.add(Bongos(context, eventsByNote(Midi.LOW_BONGO, Midi.HIGH_BONGO)))
        if (noteOnEvents.any { it.note.oneOf(Midi.TAMBOURINE) })
            instruments.add(Tambourine(context, eventsByNote(Midi.TAMBOURINE)))
        if (noteOnEvents.any { it.note.oneOf(Midi.HAND_CLAP) })
            instruments.add(HandClap(context, eventsByNote(Midi.HAND_CLAP)))
        if (noteOnEvents.any { it.note.oneOf(Midi.STICKS) })
            instruments.add(Sticks(context, eventsByNote(Midi.STICKS)))
        if (noteOnEvents.any { it.note.oneOf(Midi.JINGLE_BELL) })
            instruments.add(JingleBells(context, eventsByNote(Midi.JINGLE_BELL)))
        if (noteOnEvents.any { it.note.oneOf(Midi.CASTANETS) })
            instruments.add(Castanets(context, eventsByNote(Midi.CASTANETS)))
        if (noteOnEvents.any { it.note.oneOf(Midi.HIGH_Q) })
            instruments.add(HighQ(context, eventsByNote(Midi.HIGH_Q)))
        if (noteOnEvents.any { it.note.oneOf(Midi.HIGH_WOODBLOCK, Midi.LOW_WOODBLOCK) })
            instruments.add(Woodblock(context, eventsByNote(Midi.HIGH_WOODBLOCK, Midi.LOW_WOODBLOCK)))
        if (noteOnEvents.any { it.note.oneOf(Midi.LOW_AGOGO, Midi.HIGH_AGOGO) })
            instruments.add(Agogo(context, eventsByNote(Midi.LOW_AGOGO, Midi.HIGH_AGOGO)))
        if (noteOnEvents.any { it.note.oneOf(Midi.SHAKER) })
            instruments.add(Shaker(context, eventsByNote(Midi.SHAKER)))
        if (noteOnEvents.any { it.note.oneOf(Midi.CABASA) })
            instruments.add(Cabasa(context, eventsByNote(Midi.CABASA)))
        if (noteOnEvents.any { it.note.oneOf(Midi.MARACAS) })
            instruments.add(Maracas(context, eventsByNote(Midi.MARACAS)))
        if (noteOnEvents.any { it.note.oneOf(Midi.CLAVES) })
            instruments.add(Claves(context, eventsByNote(Midi.CLAVES)))
        if (noteOnEvents.any { it.note.oneOf(Midi.OPEN_TRIANGLE) })
            instruments.add(Triangle(context, eventsByNote(Midi.OPEN_TRIANGLE), OPEN))
        if (noteOnEvents.any { it.note.oneOf(Midi.MUTE_TRIANGLE) })
            instruments.add(Triangle(context, eventsByNote(Midi.MUTE_TRIANGLE), MUTED))
        if (noteOnEvents.any { it.note.oneOf(Midi.SQUARE_CLICK) })
            instruments.add(SquareClick(context, eventsByNote(Midi.SQUARE_CLICK)))
        if (noteOnEvents.any { it.note.oneOf(Midi.METRONOME_CLICK, Midi.METRONOME_BELL) })
            instruments.add(Metronome(context, eventsByNote(Midi.METRONOME_CLICK, Midi.METRONOME_BELL)))
        if (noteOnEvents.any { it.note.oneOf(Midi.SHORT_WHISTLE, Midi.LONG_WHISTLE) })
            instruments.add(Whistle(context, eventsByNote(Midi.SHORT_WHISTLE, Midi.LONG_WHISTLE)))
        if (noteOnEvents.any { it.note.oneOf(Midi.MUTE_SURDO, Midi.OPEN_SURDO) })
            instruments.add(Surdo(context, eventsByNote(Midi.MUTE_SURDO, Midi.OPEN_SURDO)))

        instruments.forEach {
            when (it) {
                is SnareDrum, is BassDrum, is Tom, is Cymbal, is HiHat -> drumSetNode.attachChild(it.highLevelNode)
                else -> percussionNode.attachChild(it.highLevelNode)
            }
        }

        /* Add shadow */
        val shadow = context.assetManager.loadModel("Assets/DrumShadow.obj")
        val material = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        material.setTexture("ColorMap", context.assetManager.loadTexture("Assets/DrumShadow.png"))
        material.additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        shadow.queueBucket = RenderQueue.Bucket.Transparent
        shadow.setMaterial(material)
        shadow.move(0f, 0.1f, -80f)

        percussionNode.attachChild(drumSetNode)
        percussionNode.attachChild(shadow)
        instrumentNode.attachChild(percussionNode)
    }
}

/** Given a list of integers, determines if the root integer is equal to at least one of the provided integers. */
fun Int.oneOf(vararg options: Int): Boolean = options.any { it == this }
