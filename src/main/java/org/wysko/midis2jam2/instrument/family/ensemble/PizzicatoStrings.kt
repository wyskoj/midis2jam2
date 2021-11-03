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
package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.instrument.algorithmic.VibratingStringAnimator
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave.TwelfthOfOctaveDecayed
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * Pizzicato strings have 12 separate strings that animate for each note. When a note is played, the string moves
 * forwards and vibrates for about 0.14 seconds.
 */
class PizzicatoStrings(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : DecayedInstrument(context, eventList) {

    /** Each string. */
    val strings = arrayOfNulls<PizzicatoString>(12)

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val eventsToDoOn = NoteQueue.collect(hits, context, time)

        /* Play each note that needs to be animated */
        eventsToDoOn.forEach { strings[(it.note + 3) % 12]!!.play() }

        strings.forEach { it!!.tick(delta) }
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localRotation =
            Quaternion().fromAngles(0f, rad((45f + 12 * updateInstrumentIndex(delta)).toDouble()), 0f)
    }

    /** A single string. */
    inner class PizzicatoString : TwelfthOfOctaveDecayed() {

        /** Contains the anim strings. */
        private val animStringNode = Node()

        /** The resting string. */
        private val restingString: Spatial

        /** Each frame of animation. */
        private val animStrings: Array<Spatial>

        /** Is this string currently playing? */
        var playing = false

        /** Animates the anim strings. */
        private val stringAnimator: VibratingStringAnimator

        /** The amount of progress playing the current note. */
        private var progress = 0.0

        override fun tick(delta: Float) {
            /* Tick string animation */
            stringAnimator.tick(delta)

            /* No longer playing if we have surpassed the animation time */
            if (progress >= 1) playing = false


            if (playing) {
                /* Move the string forward, show anim strings, hide resting string */
                animNode.setLocalTranslation(0f, 0f, 2f)
                animStringNode.cullHint = Dynamic
                restingString.cullHint = Always
            } else {
                /* Move the string backwards, hide anim strings, show resting string */
                animNode.setLocalTranslation(0f, 0f, 0f)
                animStringNode.cullHint = Always
                restingString.cullHint = Dynamic
            }

            /* Update progress */
            progress += (delta * 7).toDouble()
        }

        /** Begin playing this string. */
        fun play() {
            playing = true
            progress = 0.0
        }

        init {
            /* Load string holder and resting string */
            animNode.attachChild(context.loadModel("PizzicatoStringHolder.obj", "Wood.bmp"))
            restingString = context.loadModel("StageString.obj", "StageString.bmp")

            /* Load anim strings */
            animStrings = Array(5) {
                context.loadModel("StageStringBottom$it.obj", "StageStringPlaying.bmp").apply {
                    cullHint = Always // Hide on startup
                    animStringNode.attachChild(this)
                }
            }
            stringAnimator = VibratingStringAnimator(*animStrings)

            /* Attach */
            animNode.run {
                attachChild(animStringNode)
                attachChild(restingString)
            }
        }
    }

    init {
        /* Position strings */
        for (i in 0..11) {
            strings[i] = PizzicatoString().apply {
                instrumentNode.attachChild(highestLevel)
                highestLevel.setLocalTranslation(i * 2f, i * 0.5f, 0f)
                highestLevel.localScale = Vector3f(1f, 0.5f - 0.019f * i, 1f)
            }
        }

        /* Position instrument */
        instrumentNode.setLocalTranslation(0f, 6.7f, -138f)
    }
}