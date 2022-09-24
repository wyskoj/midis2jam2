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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

/**
 * The melodic woodblocks.
 */
class Woodblocks(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : TwelveDrumOctave(context, eventList, 20f) {

    override val twelfths: Array<TwelfthOfOctaveDecayed> = Array(12) {
        Woodblock(it).apply woodblock@{
            offsetNodes[it].apply {
                attachChild(this@woodblock.highestLevel)
                percussionNodes[it].attachChild(this)
            }
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        with(updateInstrumentIndex(delta)) {
            offsetNode.setLocalTranslation(0f, 15 + 3.6f * this, 0f)
            instrumentNode.localRotation =
                Quaternion().fromAngles(0f, -FastMath.HALF_PI + FastMath.HALF_PI * this, 0f)
        }
    }

    /**
     * A single woodblock.
     */
    inner class Woodblock(i: Int) : TwelfthOfOctaveDecayed() {
        init {
            animNode.attachChild(
                context.loadModel("WoodBlockSingle.obj", "SimpleWood.bmp").apply {
                    setLocalScale(1 - 0.036f * i)
                }
            )
        }
    }
}
