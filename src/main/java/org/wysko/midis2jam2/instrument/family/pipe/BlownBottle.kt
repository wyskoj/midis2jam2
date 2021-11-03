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
package org.wysko.midis2jam2.instrument.family.pipe

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior.OUTWARDS
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffType.POP
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** The Blown bottle. */
class BlownBottle(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    WrappedOctaveSustained(context, events, true) {

    /** The Bottle nodes. */
    private val bottleNodes = Array(12) { Node() }

    override fun moveForMultiChannel(delta: Float) {
        val index = updateInstrumentIndex(delta)
        offsetNode.setLocalTranslation(0f, 20 + index * 3.6f, 0f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, FastMath.HALF_PI * index, 0f)
    }

    /** A single Bottle. */
    inner class Bottle(i: Int) : TwelfthOfOctave() {

        /** The puffer that blows across the top of the bottle. */
        private val puffer: SteamPuffer = SteamPuffer(context, POP, 1.0, OUTWARDS)

        override fun play(duration: Double) {
            playing = true
            progress = 0.0
            this.duration = duration
        }

        override fun tick(delta: Float) {
            if (progress >= 1) {
                playing = false
                progress = 0.0
            }
            if (playing) {
                progress += delta / duration
            }
            puffer.tick(delta, playing)
        }

        init {
            /* Load pop bottle */
            highestLevel.attachChild(context.loadModel("PopBottle.obj", "PopBottle.bmp", MatType.REFLECTIVE, 0.9f))

            /* Load pop bottle label */
            context.loadModel("PopBottleLabel.obj", "PopLabel.bmp").apply {
                localRotation = Quaternion().fromAngles(0f, FastMath.PI, 0f)
                highestLevel.attachChild(this)
            }

            /* Load pop */
            val scale = 0.3f + 0.027273f * i
            context.loadModel("PopBottlePop.obj", "Pop.bmp", MatType.REFLECTIVE, 0.8f).apply {
                setLocalTranslation(0f, -3.25f, 0f)
                scale(1f, scale, 1f)
                highestLevel.attachChild(this)
            }

            /* Load middle */
            context.loadModel("PopBottleMiddle.obj", "PopBottle.bmp", MatType.REFLECTIVE, 0.9f).apply {
                scale(1f, 1 - scale, 1f)
                highestLevel.attachChild(this)
            }

            /* Init puffer */
            puffer.steamPuffNode.apply {
                highestLevel.attachChild(this)
                localRotation = Quaternion().fromAngles(0f, FastMath.PI, 0f)
                setLocalTranslation(1f, 3.5f, 0f)
            }
        }
    }

    init {
        twelfths = Array(12) {
            Bottle(it).apply {
                highestLevel.setLocalTranslation(-15f, 0f, 0f)
                bottleNodes[it].attachChild(this.highestLevel)
                bottleNodes[it].localRotation = Quaternion().fromAngles(0f, rad(7.5 * it), 0f)
                bottleNodes[it].setLocalTranslation(0f, 0.3f * it, 0f)
                instrumentNode.attachChild(bottleNodes[it])
            }
        }
        instrumentNode.setLocalTranslation(75f, 0f, -35f)
    }
}