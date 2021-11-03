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
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.midi.Midi
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** The hi-hat. */
class HiHat(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : SingleStickInstrument(context, hits) {

    /** The list of NoteOn events that the stick needs to worry about (closed and open). */
    private val hitsToStrike: List<MidiNoteOnEvent>

    /** The top cymbal. */
    private val topCymbal = Node()

    /** The whole hi-hat. */
    private val wholeHat = Node()

    /** The cymbal animator. */
    private val animator: CymbalAnimator

    /** The current animation time. */
    private var animTime = 0.0

    /**
     * The current status of the hi-hat.
     *
     * @see HiHatStatus
     */
    private var status = HiHatStatus.CLOSED

    override fun tick(time: Double, delta: Float) {
        animator.tick(delta)

        val recoil = NoteQueue.collectOne(hits, context, time)

        /* If a note is to be played */
        if (recoil != null) {
            /* Strike for cymbal animation */
            animator.strike()

            /* Recoil hi-hat */
            wholeHat.setLocalTranslation(0f, (-0.7 * velocityRecoilDampening(recoil.velocity)).toFloat(), -14f)

            /* Open or close hat based on note */
            if (recoil.note == Midi.OPEN_HI_HAT) {
                status = HiHatStatus.OPEN
                topCymbal.setLocalTranslation(0f, 2f, 0f)
            } else {
                status = HiHatStatus.CLOSED
                topCymbal.setLocalTranslation(0f, 1.2f, 0f)
            }

            /* Reset animation time */
            animTime = 0.0
        }
        /* Apply wobble if the hat is open */
        topCymbal.localRotation =
            Quaternion().fromAngles(if (status == HiHatStatus.CLOSED) 0f else animator.rotationAmount(), 0f, 0f)

        /* Increment anim time if there has already been a hat strike already */
        if (animTime != -1.0) animTime += delta.toDouble()

        /* Animate stick */
        handleStick(time, delta, hitsToStrike as MutableList<MidiNoteOnEvent>)

        /* Move the hat up for recoil */
        wholeHat.move(0f, 5 * delta, 0f)

        /* But not so far that it keeps going up */
        if (wholeHat.localTranslation.y > 0) {
            val localTranslation = Vector3f(wholeHat.localTranslation)
            localTranslation.also { it.y = it.y.coerceAtMost(0f) }
            wholeHat.localTranslation = localTranslation
        }
    }

    /** The status of the hi-hat. */
    private enum class HiHatStatus {
        /** The hat is closed, meaning the top cymbal and bottom cymbal are together. */
        CLOSED,

        /** The hat is open, meaning the top cymbal is raised. */
        OPEN
    }

    companion object {
        /** How fast the hi-hat wobbles. */
        private const val WOBBLE_SPEED = 10

        /** How fast the hi-hat returns to rest after being struck. */
        private const val DAMPENING = 2.0

        /** The intensity of the strike. */
        private const val AMPLITUDE = 0.25
    }

    init {
        val bottomCymbal = Node()

        /* Filter out hits that the stick needs to worry about */
        hitsToStrike = hits.filter { it.note == Midi.OPEN_HI_HAT || it.note == Midi.CLOSED_HI_HAT }

        /* Load the cymbals */
        topCymbal.attachChild(
            context.loadModel(
                "DrumSet_Cymbal.obj",
                "CymbalSkinSphereMap.bmp",
                MatType.REFLECTIVE,
                0.7f
            )
        )
        bottomCymbal.attachChild(
            context.loadModel(
                "DrumSet_Cymbal.obj",
                "CymbalSkinSphereMap.bmp",
                MatType.REFLECTIVE,
                0.7f
            ).apply {
                localRotation = Quaternion().fromAngles(rad(180.0), 0f, 0f)
            }
        )

        topCymbal.setLocalTranslation(0f, 1.2f, 0f)

        wholeHat.run {
            attachChild(topCymbal)
            attachChild(bottomCymbal)
            setLocalScale(1.3f)
            setLocalTranslation(0f, 0f, -14f)
        }

        highLevelNode.run {
            attachChild(wholeHat)
            setLocalTranslation(-6f, 22f, -72f)
            localRotation = Quaternion().fromAngles(0f, rad(90.0), 0f)
            detachChild(stickNode)
        }

        wholeHat.attachChild(stickNode)
        stickNode.setLocalTranslation(0f, 1f, 13f)
        animator = CymbalAnimator(AMPLITUDE, WOBBLE_SPEED.toDouble(), DAMPENING)
    }
}