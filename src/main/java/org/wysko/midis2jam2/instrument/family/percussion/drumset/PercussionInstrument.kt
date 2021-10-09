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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.math.FastMath
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.jetbrains.annotations.Range
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/** Anything on the percussion channel. This excludes melodic agogos, woodblocks, etc. */
abstract class PercussionInstrument protected constructor(
    context: Midis2jam2,
    /** The hits of this instrument. */
    override val hits: MutableList<MidiNoteOnEvent>
) : DecayedInstrument(context, hits) {

    /** The High level node. */
    val highLevelNode: Node = Node()

    /** The Recoil node. */
    protected val recoilNode: Node = Node()

    override fun moveForMultiChannel(delta: Float) {
        // Do nothing!
    }

    companion object {
        /** The unitless rate at which an instrument recoils. */
        const val DRUM_RECOIL_COMEBACK: Float = 22f

        /**
         * How far the drum should travel when hit.
         */
        private const val RECOIL_DISTANCE: Float = -2f

        /**
         * midis2jam2 displays velocity ramping in recoiled instruments. Different functions may be used, but a sqrt
         * regression looks pretty good. May adjust this in the future.
         * [See a graph.](https://www.desmos.com/calculator/17rgvqhl84)
         *
         * @param x the velocity of the note
         * @return a percentage to multiply by the target recoil
         */
        fun velocityRecoilDampening(x: @Range(from = 0, to = 127) Int): @Range(from = 0, to = 1) Double {
            return (FastMath.sqrt(x.toFloat()) / 11.26942767f).toDouble()
        }

        /**
         * Animates a drum recoiling. Call this method on every frame to ensure animation is handled.
         *
         * @param drum     the drum
         * @param struck   true if the drum should go down, false otherwise
         * @param velocity the velocity of the strike
         * @param delta    the amount of time since the last frame update
         */
        @JvmStatic
        fun recoilDrum(drum: Spatial, struck: Boolean, velocity: Int, delta: Float) {
            val localTranslation = drum.localTranslation
            if (localTranslation.y < -0.0001) {
                drum.setLocalTranslation(0f, 0f.coerceAtMost(localTranslation.y + DRUM_RECOIL_COMEBACK * delta), 0f)
            } else {
                drum.setLocalTranslation(0f, 0f, 0f)
            }
            if (struck) {
                drum.setLocalTranslation(
                    0f,
                    (velocityRecoilDampening(velocity) * RECOIL_DISTANCE).toFloat(),
                    0f
                )
            }
        }
    }

    init {
        instrumentNode.attachChild(recoilNode)
    }
}