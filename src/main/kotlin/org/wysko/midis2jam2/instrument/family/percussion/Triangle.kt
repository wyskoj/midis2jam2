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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.MUTE_TRIANGLE
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.cullHint

/** The Triangle. */
class Triangle(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    init {
        context.loadModel("Triangle.obj", "ShinySilver.bmp", 0.9f).also {
            recoilNode.attachChild(it)
        }
    }

    private val fist = context.loadModel("MutedTriangle.obj", "hands.bmp").also {
        recoilNode.attachChild(it)
        it.cullHint = Spatial.CullHint.Always // Start the triangle in the unmuted position
    }

    private val beater = Striker(
        context = context,
        strikeEvents = hits,
        stickModel = context.loadModel("Triangle_Stick.obj", "ShinySilver.bmp", 0.9f)
    ).apply {
        setParent(instrumentNode)
        node.move(0f, 2f, 4f)
    }

    init {
        instrumentNode.setLocalTranslation(0f, 53f, -57f)

        /* By rotating the recoil node on a 45, the direction of recoil is SW, then the whole thing is rotated back
         * so that it appears upright. */
        recoilNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(45f))
        instrumentNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(-45f))
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val results = beater.tick(time, delta)
        recoilDrum(recoilNode, results.velocity, delta)
        results.strike?.let {
            fist.cullHint = (it.note == MUTE_TRIANGLE).cullHint()
        }
    }
}
