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

package org.wysko.midis2jam2.instrument.family.soundeffects

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.instrument.RisingPitchClassAnimator
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.cullHint
import org.wysko.midis2jam2.world.modelD
import kotlin.math.sin

private val BASE_POSITION = Vector3f(0f, 49.5f, -152.65f)

private const val BIRD_TEXTURE = "Bird.png"

/**
 * The bird tweet.
 */
class BirdTweet(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    DivisiveSustainedInstrument(context, events, true) {
    override val animators: Array<PitchClassAnimator> =
        Array(12) {
            Bird(events.notePeriodsModulus(context, it))
        }

    override fun adjustForMultipleInstances(delta: Float) {
        val indexForMoving = updateInstrumentIndex(delta)
        animators.forEach {
            if (indexForMoving >= 0) {
                it.root.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, 10f, -15f).mult(indexForMoving))
            } else {
                it.root.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, indexForMoving * 10f, indexForMoving * 10f))
            }
        }
    }

    /**
     * A single bird.
     */
    inner class Bird(notePeriods: List<NotePeriod>) : RisingPitchClassAnimator(context, notePeriods) {
        /** The open beak. */
        private val openBeak =
            context.modelD("BirdBeak_Open.obj", BIRD_TEXTURE).also {
                geometry.attachChild(it)
            }

        /** The closed beak. */
        private val closedBeak =
            context.modelD("BirdBeak_Closed.obj", BIRD_TEXTURE).also {
                geometry.attachChild(it)
            }

        /** The wings. */
        private val wings =
            context.modelD("BirdWings.obj", BIRD_TEXTURE).also {
                geometry.attachChild(it)
            }

        init {
            geometry.attachChild(
                context.modelD("Bird.obj", BIRD_TEXTURE).apply {
                    this as Node
                    getChild(0).setMaterial(context.reflectiveMaterial("Assets/HornSkin.bmp"))
                },
            )
        }

        /**
         * The current animation offset, or index.
         */
        private var animationIndex = 0.0f

        override fun tick(
            time: Double,
            delta: Float,
        ) {
            super.tick(time, delta)

            openBeak.cullHint = playing.cullHint()
            closedBeak.cullHint = (!playing).cullHint()

            if (playing) {
                animationIndex += 0.3f
            } else {
                animationIndex = 0f
            }

            wings.localRotation = Quaternion().fromAngles(sin(animationIndex) * 0.5f, 0f, 0f)
        }
    }

    init {
        Array(12) {
            Node().apply {
                attachChild(animators[it].root)
                localRotation = Quaternion().fromAngles(0f, Utils.rad(-55 + it * -2.5f), 0f)
                geometry.attachChild(this)
            }
        }
    }
}
