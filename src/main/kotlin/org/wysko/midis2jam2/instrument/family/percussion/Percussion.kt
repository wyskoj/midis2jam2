/*
 * Copyright (C) 2022 Jacob Wysko
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
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.instrument.family.percussion.drumset.BassDrum
import org.wysko.midis2jam2.instrument.family.percussion.drumset.Cymbal
import org.wysko.midis2jam2.instrument.family.percussion.drumset.CymbalType
import org.wysko.midis2jam2.instrument.family.percussion.drumset.HiHat
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument
import org.wysko.midis2jam2.instrument.family.percussion.drumset.RideCymbal
import org.wysko.midis2jam2.instrument.family.percussion.drumset.SnareDrum
import org.wysko.midis2jam2.instrument.family.percussion.drumset.Tom
import org.wysko.midis2jam2.instrument.family.percussion.drumset.TomPitch
import org.wysko.midis2jam2.midi.ACOUSTIC_BASS_DRUM
import org.wysko.midis2jam2.midi.ACOUSTIC_SNARE
import org.wysko.midis2jam2.midi.CABASA
import org.wysko.midis2jam2.midi.CASTANETS
import org.wysko.midis2jam2.midi.CHINESE_CYMBAL
import org.wysko.midis2jam2.midi.CLAVES
import org.wysko.midis2jam2.midi.CLOSED_HI_HAT
import org.wysko.midis2jam2.midi.COWBELL
import org.wysko.midis2jam2.midi.CRASH_CYMBAL_1
import org.wysko.midis2jam2.midi.CRASH_CYMBAL_2
import org.wysko.midis2jam2.midi.ELECTRIC_BASS_DRUM
import org.wysko.midis2jam2.midi.ELECTRIC_SNARE
import org.wysko.midis2jam2.midi.HAND_CLAP
import org.wysko.midis2jam2.midi.HIGH_AGOGO
import org.wysko.midis2jam2.midi.HIGH_BONGO
import org.wysko.midis2jam2.midi.HIGH_FLOOR_TOM
import org.wysko.midis2jam2.midi.HIGH_Q
import org.wysko.midis2jam2.midi.HIGH_TIMBALE
import org.wysko.midis2jam2.midi.HIGH_TOM
import org.wysko.midis2jam2.midi.HIGH_WOODBLOCK
import org.wysko.midis2jam2.midi.HI_MID_TOM
import org.wysko.midis2jam2.midi.JINGLE_BELL
import org.wysko.midis2jam2.midi.LONG_GUIRO
import org.wysko.midis2jam2.midi.LONG_WHISTLE
import org.wysko.midis2jam2.midi.LOW_AGOGO
import org.wysko.midis2jam2.midi.LOW_BONGO
import org.wysko.midis2jam2.midi.LOW_CONGA
import org.wysko.midis2jam2.midi.LOW_FLOOR_TOM
import org.wysko.midis2jam2.midi.LOW_MID_TOM
import org.wysko.midis2jam2.midi.LOW_TIMBALE
import org.wysko.midis2jam2.midi.LOW_TOM
import org.wysko.midis2jam2.midi.LOW_WOODBLOCK
import org.wysko.midis2jam2.midi.MARACAS
import org.wysko.midis2jam2.midi.METRONOME_BELL
import org.wysko.midis2jam2.midi.METRONOME_CLICK
import org.wysko.midis2jam2.midi.MUTE_CUICA
import org.wysko.midis2jam2.midi.MUTE_HIGH_CONGA
import org.wysko.midis2jam2.midi.MUTE_SURDO
import org.wysko.midis2jam2.midi.MUTE_TRIANGLE
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.MidiProgramEvent
import org.wysko.midis2jam2.midi.OPEN_CUICA
import org.wysko.midis2jam2.midi.OPEN_HIGH_CONGA
import org.wysko.midis2jam2.midi.OPEN_HI_HAT
import org.wysko.midis2jam2.midi.OPEN_SURDO
import org.wysko.midis2jam2.midi.OPEN_TRIANGLE
import org.wysko.midis2jam2.midi.PEDAL_HI_HAT
import org.wysko.midis2jam2.midi.RIDE_BELL
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_1
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_2
import org.wysko.midis2jam2.midi.SHAKER
import org.wysko.midis2jam2.midi.SHORT_GUIRO
import org.wysko.midis2jam2.midi.SHORT_WHISTLE
import org.wysko.midis2jam2.midi.SIDE_STICK
import org.wysko.midis2jam2.midi.SLAP
import org.wysko.midis2jam2.midi.SPLASH_CYMBAL
import org.wysko.midis2jam2.midi.SQUARE_CLICK
import org.wysko.midis2jam2.midi.STICKS
import org.wysko.midis2jam2.midi.TAMBOURINE

/** Percussion. */
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

        NoteQueue.collectOne(programEvents, time, context)?.let { event ->
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

    /**
     * Re-textures any instruments that are [Retexturable], given the [snareTexture] and the [otherTexture].
     *
     * @see SnareDrum
     * @see Tom
     */
    @Suppress("SpellCheckingInspection")
    private fun retexture(snareTexture: String, otherTexture: String) =
        instruments.filterIsInstance<Retexturable>().forEach {
            if (it.retextureType() == RetextureType.SNARE) {
                it.drum().setMaterial(context.unshadedMaterial(snareTexture))
            } else if (it.retextureType() == RetextureType.OTHER) {
                it.drum().setMaterial(context.unshadedMaterial(otherTexture))
            }
        }

    override fun moveForMultiChannel(delta: Float) {
        percussionNode.setLocalTranslation(0f, updateInstrumentIndex(delta) * -10, 0f)
    }

    override fun toString(): String {
        return buildString {
            appendLine(instruments.joinToString(""))
        }
    }

    init {
        val drumSetNode = Node()

        instruments.add(SnareDrum(context, eventsByNote(ACOUSTIC_SNARE, ELECTRIC_SNARE, SIDE_STICK)))
        instruments.add(BassDrum(context, eventsByNote(ELECTRIC_BASS_DRUM, ACOUSTIC_BASS_DRUM)))
        instruments.add(Tom(context, eventsByNote(LOW_FLOOR_TOM), TomPitch["low_floor"]))
        instruments.add(Tom(context, eventsByNote(HIGH_FLOOR_TOM), TomPitch["high_floor"]))
        instruments.add(Tom(context, eventsByNote(LOW_TOM), TomPitch["low"]))
        instruments.add(Tom(context, eventsByNote(LOW_MID_TOM), TomPitch["low_mid"]))
        instruments.add(Tom(context, eventsByNote(HI_MID_TOM), TomPitch["high_mid"]))
        instruments.add(Tom(context, eventsByNote(HIGH_TOM), TomPitch["high"]))
        instruments.add(Cymbal(context, eventsByNote(CRASH_CYMBAL_1), CymbalType["crash_1"]))
        instruments.add(Cymbal(context, eventsByNote(CRASH_CYMBAL_2), CymbalType["crash_2"]))
        instruments.add(Cymbal(context, eventsByNote(SPLASH_CYMBAL), CymbalType["splash"]))
        instruments.add(Cymbal(context, eventsByNote(CHINESE_CYMBAL), CymbalType["china"]))

        /* Calculate ride cymbal notes */
        val rides = eventsByNote(RIDE_BELL, RIDE_CYMBAL_1, RIDE_CYMBAL_2)
        var currentRide = 1
        val ride1Notes = ArrayList<MidiNoteOnEvent>()
        val ride2Notes = ArrayList<MidiNoteOnEvent>()
        rides.forEach {
            when (it.note) {
                RIDE_CYMBAL_1 -> {
                    ride1Notes.add(it)
                    currentRide = 1
                }

                RIDE_CYMBAL_2 -> {
                    ride2Notes.add(it)
                    currentRide = 2
                }

                RIDE_BELL -> {
                    if (currentRide == 1) ride1Notes.add(it)
                    else ride2Notes.add(it)
                }
            }
        }

        instruments.add(RideCymbal(context, ride1Notes, CymbalType["ride_1"]))
        instruments.add(RideCymbal(context, ride2Notes, CymbalType["ride_2"]))
        instruments.add(HiHat(context, eventsByNote(PEDAL_HI_HAT, OPEN_HI_HAT, CLOSED_HI_HAT)))

        if (noteOnEvents.any { it.note.oneOf(SLAP) }) {
            instruments.add(Slap(context, eventsByNote(SLAP)))
        }
        if (noteOnEvents.any { it.note.oneOf(LOW_CONGA, OPEN_HIGH_CONGA, MUTE_HIGH_CONGA) }) {
            instruments.add(Congas(context, eventsByNote(LOW_CONGA, OPEN_HIGH_CONGA, MUTE_HIGH_CONGA)))
        }
        if (noteOnEvents.any { it.note.oneOf(COWBELL) }) {
            instruments.add(Cowbell(context, eventsByNote(COWBELL)))
        }
        if (noteOnEvents.any { it.note.oneOf(HIGH_TIMBALE, LOW_TIMBALE) }) {
            instruments.add(Timbales(context, eventsByNote(HIGH_TIMBALE, LOW_TIMBALE)))
        }
        if (noteOnEvents.any { it.note.oneOf(LOW_BONGO, HIGH_BONGO) }) {
            instruments.add(Bongos(context, eventsByNote(LOW_BONGO, HIGH_BONGO)))
        }
        if (noteOnEvents.any { it.note.oneOf(TAMBOURINE) }) {
            instruments.add(Tambourine(context, eventsByNote(TAMBOURINE)))
        }
        if (noteOnEvents.any { it.note.oneOf(HAND_CLAP) }) {
            instruments.add(HandClap(context, eventsByNote(HAND_CLAP)))
        }
        if (noteOnEvents.any { it.note.oneOf(STICKS) }) {
            instruments.add(Sticks(context, eventsByNote(STICKS)))
        }
        if (noteOnEvents.any { it.note.oneOf(JINGLE_BELL) }) {
            instruments.add(JingleBells(context, eventsByNote(JINGLE_BELL)))
        }
        if (noteOnEvents.any { it.note.oneOf(CASTANETS) }) {
            instruments.add(Castanets(context, eventsByNote(CASTANETS)))
        }
        if (noteOnEvents.any { it.note.oneOf(HIGH_Q) }) {
            instruments.add(HighQ(context, eventsByNote(HIGH_Q)))
        }
        if (noteOnEvents.any { it.note.oneOf(HIGH_WOODBLOCK, LOW_WOODBLOCK) }) {
            instruments.add(Woodblock(context, eventsByNote(HIGH_WOODBLOCK, LOW_WOODBLOCK)))
        }
        if (noteOnEvents.any { it.note.oneOf(LOW_AGOGO, HIGH_AGOGO) }) {
            instruments.add(Agogo(context, eventsByNote(LOW_AGOGO, HIGH_AGOGO)))
        }
        if (noteOnEvents.any { it.note.oneOf(SHAKER) }) {
            instruments.add(Shaker(context, eventsByNote(SHAKER)))
        }
        if (noteOnEvents.any { it.note.oneOf(CABASA) }) {
            instruments.add(Cabasa(context, eventsByNote(CABASA)))
        }
        if (noteOnEvents.any { it.note.oneOf(MARACAS) }) {
            instruments.add(Maracas(context, eventsByNote(MARACAS)))
        }
        if (noteOnEvents.any { it.note.oneOf(CLAVES) }) {
            instruments.add(Claves(context, eventsByNote(CLAVES)))
        }
        if (noteOnEvents.any { it.note.oneOf(OPEN_TRIANGLE, MUTE_TRIANGLE) }) {
            instruments.add(Triangle(context, eventsByNote(OPEN_TRIANGLE, MUTE_TRIANGLE)))
        }
        if (noteOnEvents.any { it.note.oneOf(SQUARE_CLICK) }) {
            instruments.add(SquareClick(context, eventsByNote(SQUARE_CLICK)))
        }
        if (noteOnEvents.any { it.note.oneOf(METRONOME_CLICK, METRONOME_BELL) }) {
            instruments.add(Metronome(context, eventsByNote(METRONOME_CLICK, METRONOME_BELL)))
        }
        if (noteOnEvents.any { it.note.oneOf(SHORT_WHISTLE, LONG_WHISTLE) }) {
            instruments.add(Whistle(context, eventsByNote(SHORT_WHISTLE, LONG_WHISTLE)))
        }
        if (noteOnEvents.any { it.note.oneOf(MUTE_SURDO, OPEN_SURDO) }) {
            instruments.add(Surdo(context, eventsByNote(MUTE_SURDO, OPEN_SURDO)))
        }
        if (noteOnEvents.any { it.note.oneOf(LONG_GUIRO, SHORT_GUIRO) }) {
            instruments.add(Guiro(context, eventsByNote(LONG_GUIRO, SHORT_GUIRO)))
        }
        if (noteOnEvents.any { it.note.oneOf(OPEN_CUICA, MUTE_CUICA) }) {
            instruments.add(Cuica(context, eventsByNote(OPEN_CUICA, MUTE_CUICA)))
        }

        instruments.forEach {
            when (it) {
                is SnareDrum, is BassDrum, is Tom, is Cymbal, is HiHat -> drumSetNode.attachChild(it.highestLevel)
                else -> percussionNode.attachChild(it.highestLevel)
            }
        }

        /* Add shadow */
        if (context.fakeShadows) { // Display fake shadow
            context.assetManager.loadModel("Assets/DrumShadow.obj").apply {
                setMaterial(
                    Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                        setTexture("ColorMap", context.assetManager.loadTexture("Assets/DrumShadow.png"))
                        additionalRenderState.blendMode = RenderState.BlendMode.Alpha
                    }
                )
            }.also {
                it.move(0f, 0.1f, -80f)
                percussionNode.attachChild(it)
            }
        }

        percussionNode.attachChild(drumSetNode)
        instrumentNode.attachChild(percussionNode)
    }
}

/** Given a list of integers, determines if the root integer is equal to at least one of the provided integers. */
fun Int.oneOf(vararg options: Int): Boolean = options.any { it == this }
