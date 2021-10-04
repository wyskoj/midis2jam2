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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.MatType.REFLECTIVE
import org.wysko.midis2jam2.util.Utils.rad

/** Cymbals are represented with this class, excluding the [HiHat]. */
open class Cymbal(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>, type: CymbalType) :
    SingleStickInstrument(context, hits) {

    /** The Cymbal node. */
    protected val cymbalNode: Node = Node()

    /** The Animator. */
    protected var animator: CymbalAnimator

    override fun tick(time: Double, delta: Float) {
        val stickStatus = handleStick(time, delta, hits)
        handleCymbalStrikes(delta, stickStatus.justStruck())
    }

    /** Handles the animation of the cymbal, striking it and updating its rotation. */
    fun handleCymbalStrikes(delta: Float, struck: Boolean) {
        /* Strike cymbal */
        if (struck) animator.strike()

        /* Update wobble */
        cymbalNode.localRotation = Quaternion().fromAngles(animator.rotationAmount(), 0f, 0f)

        /* Tick animator */
        animator.tick(delta)
    }

    /** Defines where on the drum set each cymbal is located, its rotation, size, and animation properties. */
    enum class CymbalType(
        /** The location of the cymbal. */
        val location: Vector3f,

        /** The rotation of the cymbal. */
        val rotation: Quaternion,

        /** The size scale of the cymbal. */
        val size: Float,

        /** The amplitude of wobble when struck. */
        val amplitude: Double,

        /** The speed of wobble. */
        val wobbleSpeed: Double
    ) {
        /** The Crash 1 cymbal. */
        CRASH_1(
            Vector3f(-18f, 48f, -90f),
            Quaternion().fromAngles(rad(20.0), rad(45.0), 0f),
            2.0f,
            2.5,
            4.5
        ),

        /** The Crash 2 cymbal. */
        CRASH_2(
            Vector3f(13f, 48f, -90f),
            Quaternion().fromAngles(rad(20.0), rad(-45.0), 0f),
            1.5f,
            2.5,
            5.0
        ),

        /** The Splash cymbal. */
        SPLASH(
            Vector3f(-2f, 48f, -90f),
            Quaternion().fromAngles(rad(20.0), 0f, 0f),
            1.0f,
            2.0,
            5.0
        ),

        /** The Ride 1 cymbal. */
        RIDE_1(
            Vector3f(22f, 43f, -77.8f),
            Quaternion().fromAngles(rad(17.0), rad(291.0), rad(-9.45)),
            2f,
            0.5,
            3.0
        ),

        /** The Ride 2 cymbal. */
        RIDE_2(
            Vector3f(-23f, 40f, -78.8f),
            Quaternion().fromAngles(rad(20.0), rad(37.9), rad(-3.49)),
            2f,
            0.5,
            3.0
        ),

        /** The China cymbal. */
        CHINA(
            Vector3f(32.7f, 34.4f, -68.4f),
            Quaternion().fromAngles(rad(18.0), rad(-89.2), rad(-10.0)),
            2.0f,
            2.0,
            5.0
        );

        /** How quickly the cymbal returns to rest. */
        val dampening: Double = 1.5
    }

    init {
        /* Load cymbal model */
        val cymbal = if (type == CymbalType.CHINA) {
            context.loadModel("DrumSet_ChinaCymbal.obj", "CymbalSkinSphereMap.bmp", REFLECTIVE, 0.7f)
        } else {
            context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp", REFLECTIVE, 0.7f)
        }

        cymbalNode.run {
            attachChild(cymbal)
            setLocalScale(type.size)
        }
        highLevelNode.run {
            localTranslation = type.location
            localRotation = type.rotation
            attachChild(cymbalNode)
        }
        stick.run {
            setLocalTranslation(0f, 0f, 0f)
            setLocalTranslation(0f, 0f, -2.6f)
            localRotation = Quaternion().fromAngles(rad(-20.0), 0f, 0f)
        }
        stickNode.setLocalTranslation(0f, 2f, 18f)
        animator = CymbalAnimator(type.amplitude, type.wobbleSpeed, type.dampening)
    }
}