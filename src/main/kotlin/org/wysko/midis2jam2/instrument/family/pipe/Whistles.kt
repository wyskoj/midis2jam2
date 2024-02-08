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
package org.wysko.midis2jam2.instrument.family.pipe

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior.OUTWARDS
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffTexture.WHISTLE
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelR

/** The whistles. */
class Whistles(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    DivisiveSustainedInstrument(context, events, true) {
    /** The Whistle nodes. */
    private val whistleNodes = Array(12) { Node() }

    override val animators: Array<PitchClassAnimator> =
        Array(12) {
            Whistle(it, events.notePeriodsModulus(context, it)).apply {
                root.setLocalTranslation(-12f, 0f, 0f)
            }
        }

    override fun adjustForMultipleInstances(delta: Float) {
        val index = updateInstrumentIndex(delta)
        root.setLocalTranslation(0f, 22.5f + index * 6.8f, 0f)
        geometry.localRotation = Quaternion().fromAngles(0f, FastMath.HALF_PI * index, 0f)
    }

    /** A single Whistle. */
    inner class Whistle(i: Int, notePeriodsModulus: List<NotePeriod>) : PitchClassAnimator(context, notePeriodsModulus) {
        /** The Puffer. */
        private val puffer: SteamPuffer = SteamPuffer(context, WHISTLE, 1.0, OUTWARDS)

        override fun tick(
            time: Double,
            delta: Float,
        ) {
            super.tick(time, delta)
            if (playing) {
                val progress = collector.currentNotePeriods.first().progress(time)
                geometry.setLocalTranslation(0f, 2 - 2 * progress.toFloat(), 0f)
            } else {
                geometry.setLocalTranslation(0f, 0f, 0f)
            }
            puffer.tick(delta, playing)
        }

        init {
            context.modelR("Whistle.obj", "ShinySilver.bmp").apply {
                val scale = 2 + -0.0909091f * i
                geometry.attachChild(this)
                localRotation = Quaternion().fromAngles(0f, -FastMath.HALF_PI, 0f)
                setLocalScale(1f, scale, 1f)
                setLocalTranslation(0f, 5 + -5 * scale, 0f)
            }
            geometry.attachChild(puffer.root)
            puffer.root.run {
                localRotation = Quaternion().fromAngles(0f, FastMath.PI, 0f)
                setLocalTranslation(-1f, 3f + i * 0.1f, 0f)
            }
        }
    }

    init {
        whistleNodes.forEachIndexed { index, node ->
            node.attachChild(animators[index].root)
            node.localRotation = Quaternion().fromAngles(0f, rad(7.5 * index), 0f)
            node.setLocalTranslation(0f, 0.1f * index, 0f)
            geometry.attachChild(node)
        }

        geometry.setLocalTranslation(75f, 0f, -35f)
    }
}
