/*
 * Copyright (C) 2023 Jacob Wysko
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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.world.Axis
import kotlin.math.abs

private const val SQUARE_ROOT_OF_127: Float = 11.269427f

/** Anything on the percussion channel. This excludes melodic agogos, woodblocks, etc. */
abstract class PercussionInstrument protected constructor(
    context: Midis2jam2,
    /** The hits of this instrument. */
    override val hits: MutableList<MidiNoteOnEvent>
) : DecayedInstrument(context, hits) {

    /** The Recoil node. */
    protected val recoilNode: Node = Node()

    override fun moveForMultiChannel(delta: Float) {
        // Do nothing!
    }

    companion object {
        /** The unitless rate at which an instrument recoils. */
        private const val DRUM_RECOIL_COMEBACK: Float = 22f

        /** How far the drum should travel when hit. */
        private const val RECOIL_DISTANCE: Float = -2f

        /**
         * midis2jam2 displays velocity ramping in recoiled instruments. Different functions may be used, but a sqrt
         * regression looks pretty good. May adjust this in the future.
         * [See a graph.](https://www.desmos.com/calculator/17rgvqhl84)
         *
         * @param x the velocity of the note
         * @return a percentage to multiply by the target recoil
         */
        fun velocityRecoilDampening(x: Int): Double = (FastMath.sqrt(x.toFloat()) / SQUARE_ROOT_OF_127).toDouble()

        /**
         * Animates a drum recoiling. Call this method on every frame to ensure animation is handled.
         *
         * @param drum     the drum
         * @param struck   true if the drum should go down, false otherwise
         * @param velocity the velocity of the strike
         * @param delta    the amount of time since the last frame update
         */
        @Deprecated(
            message = "Struck is now inferred from velocity.",
            replaceWith = ReplaceWith("recoilDrum(drum, velocity, delta)")
        )
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

        /**
         * Animates a drum recoiling. Call this method on every frame to ensure animation is handled.
         *
         * @param drum     the drum
         * @param velocity the velocity of the strike
         * @param delta    the amount of time since the last frame update
         */
        fun recoilDrum(
            drum: Spatial,
            velocity: Int,
            delta: Float,
            recoilDistance: Float = RECOIL_DISTANCE,
            recoilSpeed: Float = DRUM_RECOIL_COMEBACK,
            recoilAxis: Axis = Axis.Y
        ) {
            drum.localTranslation[recoilAxis.componentIndex] =
                (drum.localTranslation[recoilAxis.componentIndex] + recoilSpeed * delta).coerceAtMost(0f)
            if (velocity != 0) {
                drum.localTranslation[recoilAxis.componentIndex] =
                    (velocityRecoilDampening(velocity) * -abs(recoilDistance)).toFloat()
            }
        }
    }

    init {
        instrumentNode.attachChild(recoilNode)
    }
}
