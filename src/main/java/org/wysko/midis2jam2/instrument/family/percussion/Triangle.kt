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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The triangle. */
class Triangle(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>, type: TriangleType) :
    NonDrumSetPercussion(context, hits) {

    /** The Triangle node. */
    private val triangleNode = Node()

    /** The Beater node. */
    private val beaterNode = Node()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val stickStatus =
            Stick.handleStick(context, beaterNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)
        recoilDrum(
            triangleNode,
            stickStatus.justStruck(),
            if (stickStatus.strike == null) 0 else stickStatus.strike.velocity,
            delta
        )
    }

    /** The type of triangle. */
    enum class TriangleType(internal val modelFile: String) {
        /** Open triangle type. */
        OPEN("Triangle.obj"),

        /** Muted triangle type. */
        MUTED("MutedTriangle.fbx");
    }

    init {

        /* Load triangle */
        val triangle = context.loadModel(type.modelFile, "ShinySilver.bmp", MatType.REFLECTIVE, 0.9f)
        triangleNode.attachChild(triangle)

        /* Fix material if a muted triangle */
        if (type == TriangleType.MUTED) {
            val hands = context.unshadedMaterial("Assets/hands.bmp")
            (triangle as Node).getChild(1).setMaterial(hands)
        }

        /* Load beater */
        beaterNode.attachChild(
            context.loadModel(
                "Triangle_Stick.obj",
                "ShinySilver.bmp",
                MatType.REFLECTIVE,
                0.9f
            )
        )
        beaterNode.setLocalTranslation(0f, 2f, 4f)

        /* Attach nodes and position */
        instrumentNode.attachChild(triangleNode)
        instrumentNode.attachChild(beaterNode)
        triangleNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(45.0))
        instrumentNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(-45.0))
        if (type == TriangleType.OPEN) {
            instrumentNode.setLocalTranslation(-5f, 53f, -57f)
        } else {
            instrumentNode.setLocalTranslation(5f, 53f, -57f)
        }
    }
}