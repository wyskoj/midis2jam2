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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.SlidePositionManager
import org.wysko.midis2jam2.instrument.clone.StretchyClone
import org.wysko.midis2jam2.instrument.family.brass.Trombone.TromboneClone
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

private val SLIDE_MANAGER: SlidePositionManager = SlidePositionManager.from(Trombone::class.java)

/**
 * The Trombone.
 *
 * The Trombone animates by moving a slide to represent the pitch of the current note. When playing, the Trombone
 * also slightly rotates. It uses a [SlidePositionManager] to determine the correct slide position for each note.
 *
 * Because each note can have a few valid positions, [TromboneClone.getSlidePositionFromNote] finds the closest valid
 * position.
 *
 * When the Trombone is not playing and there is an upcoming note, the slide will gradually move to the next position
 * when there is less than one second between now and the note start.
 */
class Trombone(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    MonophonicInstrument(context, eventList, TromboneClone::class.java, SLIDE_MANAGER) {

    private var pitchBendAmount = 0f

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 10 * updateInstrumentIndex(delta), 0f)
    }

    override fun handlePitchBend(time: Double, delta: Float) {
        pitchBendAmount = pitchBendModulationController.tick(time, delta, false) {
            clones.any { it.isPlaying }
        }
    }

    /** A single Trombone. */
    inner class TromboneClone : StretchyClone(this@Trombone, 0.1f, 1f, Axis.Z, Axis.X) {

        /** The slide slides in and out to represent different pitches on the Trombone. */
        private val slide: Spatial =
            context.loadModel("TromboneSlide.obj", "HornSkin.bmp", 0.9f)

        /** Moves the slide of the trombone to a given position, from 1st to 7th position. */
        private fun moveToPosition(position: Double) {
            slide.localTranslation =
                slidePosition((position - pitchBendAmount).coerceIn(0.5..7.0)) // Slightly out of range still works
        }

        /**
         * Returns the 3D vector for a given slide position.
         */
        private fun slidePosition(position: Double): Vector3f = Vector3f(0f, 0f, (3.333333 * position - 1).toFloat())

        /**
         * Returns the current slide position, calculated from the current Z-axis translation.
         */
        private val currentSlidePosition: Double
            get() = 0.3 * (slide.localTranslation.z + 1)

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            if (!isPlaying) {
                advanceSlide(time, delta)
                return
            }

            notePeriodCollector.peek()?.let { // There is a note ahead
                // Buffer room lets slide move at the end of a note to get to the next if we are still playing. We'll only
                // also do this if the note is long enough (so that notes in rapid succession are animated desirably).
                if (it.startTime - time < 0.1 && currentNotePeriod?.duration()!! >= 0.15) {
                    advanceSlide(time, delta)
                } else {
                    snapSlide()
                }
            }
        }

        private fun snapSlide(): Boolean {
            moveToPosition(getSlidePositionFromNote(currentNotePeriod ?: return true).toDouble())
            return false
        }

        /**
         * Advances the trombone slide per frame if there is less than one second until the next NotePeriod.
         */
        private fun advanceSlide(time: Double, delta: Float) {
            notePeriodCollector.peek()?.let {
                /* If within Trombone range */
                if (it.midiNote in 21..80) {
                    val startTime = it.startTime

                    /* Slide only if there is 1 or fewer seconds between
                    now and the beginning of the note, but at least the delta. */
                    if (startTime - time in delta..1.0F) {
                        val targetPos = getSlidePositionFromNote(it)
                        moveToPosition(currentSlidePosition + (targetPos - currentSlidePosition) / (startTime - time) * delta)
                    }
                }
            }
        }

        /** Gets the best slide position from a NotePeriod. */
        private fun getSlidePositionFromNote(period: NotePeriod): Int {
            /* If out of range, return current position */
            val positionList = SLIDE_MANAGER.fingering(period.midiNote) ?: return currentSlidePosition.roundToInt()

            /* If there is just one valid position for this note, use that. */
            if (positionList.size == 1) return positionList[0]

            /* There are more; find the one closest to the current position. */
            /* Key = score; Value = position */
            val positionScoreMap: TreeMap<Double, Int> = TreeMap()

            /* Load all positions and their scores into the map */
            positionList.forEach { pos ->
                positionScoreMap[abs(currentSlidePosition - pos)] = pos
            }

            /* Return the first entry in the map, which is the position with the lowest score */
            return positionScoreMap.firstEntry().value
        }

        override fun moveForPolyphony(delta: Float) {
            offsetNode.localRotation = Quaternion().fromAngles(0f, rad((-70 + indexForMoving() * 15f).toDouble()), 0f)
            offsetNode.setLocalTranslation(0f, indexForMoving(), 0f)
        }

        init {
            /* Attach body, slide, and set rotation of Trombone */
            modelNode.run {
                attachChild(
                    context.loadModel("TromboneBody.obj", "HornSkin.bmp", 0.9f).apply {
                        this as Node
                        getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))
                    }
                )
                attachChild(slide)
                localRotation = Quaternion().fromAngles(rad(-10.0), 0f, 0f)
                localScale = Vector3f(0.8f, 0.8f, 0.8f)
            }

            with(bell) {
                attachChild(
                    context.loadModel("TromboneHorn.obj", "HornSkin.bmp", 0.9f)
                )
                localTranslation = Vector3f(0.5522f, 4.291f, 1.207f)
                localRotation = Quaternion().fromAngles(rad(-4.0), 0f, 0f)
            }

            /* Position Trombone */
            highestLevel.setLocalTranslation(0f, 65f, -55.4f)
        }

        override fun toString(): String {
            return super.toString() + buildString {
                append(debugProperty("slide", currentSlidePosition.toFloat()))
            }
        }
    }
}