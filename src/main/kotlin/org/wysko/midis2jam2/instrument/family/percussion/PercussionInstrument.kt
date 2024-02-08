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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis

private const val SQUARE_ROOT_OF_127: Float = 11.269427f

/** Anything on the percussion channel. */
abstract class PercussionInstrument protected constructor(
    context: Midis2jam2,
    hits: MutableList<MidiNoteOnEvent>,
) : DecayedInstrument(context, hits) {

    /** Node that recoils when the instrument is hit. */
    protected val recoilNode: Node = with(geometry) { +node() }

    override fun adjustForMultipleInstances(delta: Float) {
        root.loc = v3(0, 10 * updateInstrumentIndex(delta), 0)
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
         * @param velocity the velocity of the strike
         * @param delta    the amount of time since the last frame update
         */
        fun recoilDrum(
            drum: Spatial,
            velocity: Int,
            delta: Float,
            recoilDistance: Float = RECOIL_DISTANCE,
            recoilSpeed: Float = DRUM_RECOIL_COMEBACK,
            recoilAxis: Axis = Axis.Y,
        ) {
            if (velocity != 0) {
                drum.localTranslation =
                    recoilAxis.identity.mult((recoilDistance * velocityRecoilDampening(velocity)).toFloat())
            } else {
                drum.move(recoilAxis.identity.mult(delta * recoilSpeed))
                if (drum.localTranslation[recoilAxis.componentIndex] > 0) drum.localTranslation = Vector3f.ZERO
            }
        }
    }
}
