/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.instrument.RisingPitchClassAnimator
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.util.*
import kotlin.math.sin
import kotlin.time.Duration

private val BASE_POSITION = Vector3f(0f, 49.5f, -152.65f)

/**
 * The bird tweet.
 */
class BirdTweet(context: PerformanceManager, events: List<MidiEvent>) : DivisiveSustainedInstrument(context, events) {

    override val animators: List<PitchClassAnimator> = List(12) { Bird(events.notePeriodsModulus(context, it)) }

    init {
        with(geometry) {
            repeat(12) {
                +node {
                    +animators[it].root
                    rot = v3(0, -55 + it * -2.5, 0)
                }
            }
        }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        val indexForMoving = updateInstrumentIndex(delta)
        animators.forEach {
            if (indexForMoving >= 0) {
                it.root.localTranslation = BASE_POSITION.clone().add(Vector3f(0f, 10f, -15f).mult(indexForMoving))
            } else {
                it.root.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, indexForMoving * 10f, indexForMoving * 10f))
            }
        }
    }

    private inner class Bird(notePeriods: List<TimedArc>) : RisingPitchClassAnimator(context, notePeriods) {
        private val openBeak = context.model(Models.SoundEffects.BirdTweet.BeakOpen).also { geometry += it }
        private val closedBeak = context.model(Models.SoundEffects.BirdTweet.BeakClosed).also { geometry += it }
        private val wings = context.model(Models.SoundEffects.BirdTweet.Wings).also { geometry += it }
        private var animationIndex = 0.0f

        init {
            with(geometry) {
                +context.model(Models.SoundEffects.BirdTweet.Body)
            }
        }

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)

            openBeak.cullHint = playing.ch
            closedBeak.cullHint = (!playing).ch

            if (playing) {
                animationIndex += 0.3f
            } else {
                animationIndex = 0f
            }

            wings.rot = v3(sin(animationIndex) * 30, 0, 0)
        }
    }
}
