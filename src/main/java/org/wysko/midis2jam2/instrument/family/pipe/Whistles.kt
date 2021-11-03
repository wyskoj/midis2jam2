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
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffType.WHISTLE
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** The whistles. */
class Whistles(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    WrappedOctaveSustained(context, events, true) {

    /** The Whistle nodes. */
    private val whistleNodes = Array(12) { Node() }

    override fun moveForMultiChannel(delta: Float) {
        val index = updateInstrumentIndex(delta)
        offsetNode.setLocalTranslation(0f, 22.5f + index * 6.8f, 0f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, FastMath.HALF_PI * index, 0f)
    }

    /** A single Whistle. */
    inner class Whistle(i: Int) : TwelfthOfOctave() {

        /** The Puffer. */
        private val puffer: SteamPuffer = SteamPuffer(context, WHISTLE, 1.0, OUTWARDS)

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
                animNode.setLocalTranslation(0f, 2 - 2 * progress.toFloat(), 0f)
            } else {
                animNode.setLocalTranslation(0f, 0f, 0f)
            }
            puffer.tick(delta, playing)
        }

        init {
            context.loadModel("Whistle.obj", "ShinySilver.bmp", MatType.REFLECTIVE, 0.9f).apply {
                val scale = 2 + -0.0909091f * i
                animNode.attachChild(this)
                localRotation = Quaternion().fromAngles(0f, -FastMath.HALF_PI, 0f)
                setLocalScale(1f, scale, 1f)
                setLocalTranslation(0f, 5 + -5 * scale, 0f)
            }
            animNode.attachChild(puffer.steamPuffNode)
            puffer.steamPuffNode.run {
                localRotation = Quaternion().fromAngles(0f, FastMath.PI, 0f)
                setLocalTranslation(-1f, 3f + i * 0.1f, 0f)
            }
        }
    }

    init {
        twelfths = Array(12) {
            Whistle(it).apply {
                highestLevel.setLocalTranslation(-12f, 0f, 0f)
            }
        }

        whistleNodes.forEachIndexed { index, node ->
            node.attachChild(twelfths[index].highestLevel)
            node.localRotation = Quaternion().fromAngles(0f, rad(7.5 * index), 0f)
            node.setLocalTranslation(0f, 0.1f * index, 0f)
            instrumentNode.attachChild(node)
        }

        instrumentNode.setLocalTranslation(75f, 0f, -35f)
    }
}