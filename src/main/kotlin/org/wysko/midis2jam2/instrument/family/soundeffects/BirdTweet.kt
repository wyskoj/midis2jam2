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

package org.wysko.midis2jam2.instrument.family.soundeffects

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.WrappedOctaveSustained
import org.wysko.midis2jam2.instrument.family.brass.BouncyTwelfth
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.cullHint
import kotlin.math.sin

private val BASE_POSITION = Vector3f(0f, 49.5f, -152.65f)

private const val BIRD_TEXTURE = "Bird.png"

/**
 * The bird tweet.
 */
class BirdTweet(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) :
    WrappedOctaveSustained(context, events, true) {

    override val twelfths: Array<TwelfthOfOctave> = Array(12) {
        Bird()
    }

    override fun moveForMultiChannel(delta: Float) {
        val indexForMoving = updateInstrumentIndex(delta)
        twelfths.forEach {
            it as Bird
            if (indexForMoving >= 0) {
                it.highestLevel.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, 10f, -15f).mult(indexForMoving))
            } else {
                it.highestLevel.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, indexForMoving * 10f, indexForMoving * 10f))
            }
        }
    }


    /**
     * A single bird.
     */
    inner class Bird : BouncyTwelfth() {

        /** The open beak. */
        private val openBeak = context.loadModel("BirdBeak_Open.fbx", BIRD_TEXTURE).also {
            animNode.attachChild(it)
        }

        /** The closed beak. */
        private val closedBeak = context.loadModel("BirdBeak_Closed.fbx", BIRD_TEXTURE).also {
            animNode.attachChild(it)
        }

        /** The wings. */
        private val wings = context.loadModel("BirdWings.fbx", BIRD_TEXTURE).also {
            animNode.attachChild(it)
        }

        init {
            animNode.attachChild(context.loadModel("Bird.fbx", BIRD_TEXTURE).apply {
                this as Node
                getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkin.bmp"))
            })
        }

        /**
         * The current animation offset, or index.
         */
        private var animationIndex = 0.0f

        override fun tick(delta: Float) {
            super.tick(delta)

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
                attachChild(twelfths[it].highestLevel)
                localRotation = Quaternion().fromAngles(0f, Utils.rad(-55 + it * -2.5f), 0f)
                instrumentNode.attachChild(this)
            }
        }
    }
}