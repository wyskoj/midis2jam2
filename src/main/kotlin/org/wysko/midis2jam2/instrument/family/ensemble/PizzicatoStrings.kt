/*
 * Copyright (C) 2024 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave.TwelfthOfOctaveDecayed
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.scale
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD

/**
 * Pizzicato strings have 12 separate strings that animate for each note. When a note is played, the string moves
 * forwards and vibrates for about 0.14 seconds.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 */
class PizzicatoStrings(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : DecayedInstrument(context, eventList) {

    private val eventCollector: EventCollector<MidiNoteOnEvent> =
        EventCollector(eventList.filterIsInstance<MidiNoteOnEvent>(), context)

    private val strings: Array<PizzicatoString> = Array(12) {
        PizzicatoString().apply {
            with(highestLevel) {
                geometry += this
                loc = v3(2 * it, 0.5 * it, 0)
                scale = v3(1, 0.5 - 0.019 * it, 1)
            }
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        eventCollector.advanceCollectAll(time).forEach { strings[(it.note + 3) % 12].play() }
        strings.forEach { it.tick(delta) }
    }

    override fun adjustForMultipleInstances(delta: Float) {
        root.rot = v3(0, 45 + 12 * updateInstrumentIndex(delta), 0)
    }

    init {
        placement.loc = v3(0, 6.7, -138)
    }

    /**
     * Contains a single string.
     */
    private inner class PizzicatoString : TwelfthOfOctaveDecayed() {

        private val animatedStringNode = node()
        private val restingString: Spatial = context.modelD("StageString.obj", "StageString.bmp")
        private val animatedStringFrames: List<Spatial> = List(5) {
            context.modelD("StageStringBottom$it.obj", "StageStringPlaying.bmp").apply {
                cullHint = Always // Hide on startup
                animatedStringNode += this
                (this as Geometry).material.setColor("GlowColor", STRING_GLOW)
            }
        }
        private var playing: Boolean = false
        private val stringAnimator: StringVibrationController = StringVibrationController(animatedStringFrames)
        private var progress = 0.0
        private val nudgeCtrl = NumberSmoother(0f, 40.0)

        init {
            with(animNode) {
                +context.modelD("PizzicatoStringHolder.obj", "Wood.bmp")
                +restingString
                +animatedStringNode
            }
        }

        override fun tick(delta: Float) {
            stringAnimator.tick(delta)
            if (progress >= 1) playing = false

            // Move string and update visibility
            animatedStringNode.cullHint = playing.ch
            restingString.cullHint = (!playing).ch
            animNode.loc = v3(0, 0, nudgeCtrl.tick(delta) { if (playing) 2f else 0f })

            progress += (delta * 7).toDouble()
        }

        fun play() {
            playing = true
            progress = 0.0
        }
    }
}
