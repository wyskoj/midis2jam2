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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.SlidePositionManager
import org.wysko.midis2jam2.instrument.clone.CloneWithBell
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelR
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

private val SLIDE_MANAGER: SlidePositionManager = SlidePositionManager.from(Trombone::class)

/**
 * The Trombone.
 */
class Trombone(context: Midis2jam2, eventList: List<MidiEvent>) :
    MonophonicInstrument(context, eventList, TromboneClone::class, SLIDE_MANAGER) {

    private var bend = 0f

    override fun adjustForMultipleInstances(delta: Duration) {
        root.loc = v3(0, 10 * updateInstrumentIndex(delta), 0)
    }

    override fun handlePitchBend(time: Duration, delta: Duration, isNewNote: Boolean) {
        bend = pitchBendModulationController.tick(time, delta, false) {
            clones.any { it.isPlaying }
        }
    }

    /**
     * A Trombone clone.
     */
    inner class TromboneClone : CloneWithBell(
        parent = this@Trombone, rotationFactor = 0.1f, stretchFactor = 1f, scaleAxis = Axis.Z, rotationAxis = Axis.X
    ) {

        private val slide: Spatial = context.modelR("TromboneSlide.obj", "HornSkin.bmp")

        private val slidePosition: Double
            get() = 0.3 * (slide.localTranslation.z + 1)

        init {
            with(geometry) {
                +slide
                +context.modelR("TromboneBody.obj", "HornSkin.bmp").apply {
                    this as Node
                    getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))
                }
                rot = v3(-10.0, 0, 0)
                localScale = v3(0.8f, 0.8f, 0.8f)
            }

            with(bell) {
                +context.modelR("TromboneHorn.obj", "HornSkin.bmp")
                loc = v3(0.5522, 4.291, 1.207)
                rot = v3(-4.0, 0, 0)
            }

            highestLevel.loc = v3(0, 65, -55.4)
        }

        private fun moveToPosition(position: Double) {
            slide.loc = slideLocation((position - bend).coerceIn(0.5..7.0)) // Slightly out of range still works
        }

        private fun slideLocation(position: Double): Vector3f = Vector3f(0f, 0f, (3.333333 * position - 1).toFloat())

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)

            if (!isPlaying) {
                advanceSlide(time, delta)
                return
            }

            timedArcCollector.peek()?.let {
                /* Buffer room lets slide move at the end of a note
                 * to get to the next if we are still playing.
                 *
                 * We'll only also do this if the note is long enough
                 * (so that notes in rapid succession are animated desirably).
                 */
                if (it.startTime - time < 0.1.seconds && currentNotePeriod?.duration!! >= 0.15.seconds) {
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

        private fun advanceSlide(time: Duration, delta: Duration) {
            timedArcCollector.peek()?.let {
                if (it.note !in 21..80) {
                    return
                }
                if (it.startTime - time in delta..1.0.seconds) {
                    val target = getSlidePositionFromNote(it)
                    moveToPosition(
                        slidePosition + (target - slidePosition) / (it.startTime - time).toDouble(SECONDS) * delta.toDouble(
                            SECONDS
                        )
                    )
                }
            }
        }

        private fun getSlidePositionFromNote(period: TimedArc): Int {
            val positionList = SLIDE_MANAGER.fingering(period.note) ?: return slidePosition.roundToInt()
            if (positionList.size == 1) return positionList[0]

            // Check all positions and find the closest
            val positionScoreMap: TreeMap<Double, Int> = TreeMap()
            positionList.forEach { pos ->
                positionScoreMap[abs(slidePosition - pos)] = pos
            }
            return positionScoreMap.firstEntry().value
        }

        override fun adjustForPolyphony(delta: Duration) {
            root.loc = v3(0, indexForMoving(), 0)
            root.rot = v3(0, -70 + indexForMoving() * 15, 0)
        }

        override fun toString(): String = super.toString() + debugProperty("slide", slidePosition)
    }
}
