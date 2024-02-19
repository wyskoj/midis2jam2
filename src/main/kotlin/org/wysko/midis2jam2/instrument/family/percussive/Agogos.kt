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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.world.modelR

/** The Melodic Agogos. */
class Agogos(
    context: Midis2jam2,
    eventList: List<MidiChannelEvent>
) : TwelveDrumOctave(context, eventList, pivotOffset = 17f) {

    override val twelfths: Array<TwelfthOfOctaveDecayed> = Array(12) {
        Agogo(it).apply agogo@{
            offsetNodes[it].apply {
                attachChild(this@agogo.highestLevel)
                percussionNodes[it].attachChild(this)
            }
        }
    }

    override fun adjustForMultipleInstances(delta: Float) {
        with(updateInstrumentIndex(delta)) {
            root.setLocalTranslation(0f, 18 + 3.6f * this, 0f)
            geometry.localRotation =
                Quaternion().fromAngles(0f, -FastMath.HALF_PI + FastMath.HALF_PI * this, 0f)
        }
    }

    /** A single agogo. */
    inner class Agogo(i: Int) : TwelfthOfOctaveDecayed() {
        init {
            animNode.attachChild(
                context.modelR("AgogoSingle.obj", "HornSkinGrey.bmp").apply {
                    setLocalScale(1 - 0.036f * i)
                }
            )
        }
    }
}
