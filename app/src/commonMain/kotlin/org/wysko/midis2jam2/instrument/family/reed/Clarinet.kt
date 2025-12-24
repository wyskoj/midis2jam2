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
package org.wysko.midis2jam2.instrument.family.reed

import com.jme3.math.Quaternion
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.BellStretcher
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Companion.from
import org.wysko.midis2jam2.instrument.algorithmic.StandardBellStretcher
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.CloneWithHands
import org.wysko.midis2jam2.instrument.family.pipe.InstrumentWithHands
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

private val FINGERING_MANAGER: HandPositionFingeringManager = from(Clarinet::class)

/**
 * The clarinet has both hand positions and a stretchy bell.
 *
 * [Fingering chart](https://bit.ly/34Quj4e)
 */
class Clarinet(context: PerformanceManager, eventList: List<MidiEvent>) :
    InstrumentWithHands(
        context,
        /* Strip notes outside of standard range */
        eventList.filterIsInstance<NoteEvent>().filter { it.note in 50..90 }
            .plus(eventList.filter { it !is NoteEvent }),
        ClarinetClone::class,
        FINGERING_MANAGER
    ) {

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(reversed = true)

    override fun adjustForMultipleInstances(delta: Duration) {
        root.setLocalTranslation(0f, 20 * updateInstrumentIndex(delta), 0f)
    }

    /** The type Clarinet clone. */
    inner class ClarinetClone : CloneWithHands(this@Clarinet, 0.075f) {

        override val leftHands: List<Spatial> = List(20) {
            parent.context.modelD("ClarinetLeftHand$it.obj", "hands.bmp")
        }

        override val rightHands: List<Spatial> = List(13) {
            parent.context.modelD("ClarinetRightHand$it.obj", "hands.bmp")
        }

        init {
            /* Load body */
            geometry.attachChild(context.modelD("ClarinetBody.obj", "ClarinetSkin.png"))

            /* Position Clarinet */
            animNode.setLocalTranslation(0f, 0f, 10f)
            highestLevel.localRotation = Quaternion().fromAngles(rad(-25.0), rad(45.0), 0f)

            loadHands()
        }

        /** The bell. */
        private val bell: Spatial = context.modelD("ClarinetHorn.obj", "ClarinetSkin.png").apply {
            geometry.attachChild(this)
            setLocalTranslation(0f, -20.7125f, 0f)
        }

        /** The bell stretcher. */
        private val bellStretcher: BellStretcher = StandardBellStretcher(0.7f, Axis.Y, bell)

        override fun adjustForPolyphony(delta: Duration) {
            root.localRotation = Quaternion().fromAngles(0f, rad((25 * indexForMoving()).toDouble()), 0f)
        }

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)

            /* Stretch bell */
            bellStretcher.tick(currentNotePeriod, time)
        }

        override fun toString(): String {
            return super.toString() + buildString {
                append(debugProperty("stretch", (bellStretcher as StandardBellStretcher).scale))
            }
        }
    }

    init {
        geometry.setLocalTranslation(-25f, 50f, 0f)
        geometry.setLocalScale(0.9f)
    }
}
