/*
 * Copyright (C) 2026 Jacob Wysko
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
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.assets.Models
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.instrument.algorithmic.StandardBellStretcher
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.CloneWithHands
import org.wysko.midis2jam2.instrument.family.pipe.InstrumentWithHands
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.model
import kotlin.time.Duration

private val FINGERING_MANAGER: HandPositionFingeringManager = HandPositionFingeringManager.from(Bassoon::class)

/** The bassoon's written range, B♭1 to E5; notes outside it aren't shown. */
private val RANGE = 34..76

/**
 * Where the bell joint sits on the long joint, relative to the tip of the reed (the model's origin). The bell is
 * modeled from its own base, so it stretches up from the joint.
 */
private val BELL_POSITION = v3(1.65, 4.7347, 9.1883)

/**
 * The bassoon. Hands finger each note on the wing joint (left) and boot (right), thumbs and little fingers
 * included, from the standard fingering chart.
 *
 * [Fingering chart](https://www.wfg.woodwind.org/bassoon/)
 */
class Bassoon(context: PerformanceManager, eventList: List<MidiEvent>) :
    InstrumentWithHands(
        context,
        eventList.filterIsInstance<NoteEvent>().filter { it.note in RANGE } + eventList.filter { it !is NoteEvent },
        BassoonClone::class,
        FINGERING_MANAGER
    ) {

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(reversed = true)

    override fun adjustForMultipleInstances(delta: Duration) {
        root.loc = MULTIPLE_INSTANCE_OFFSET.mult(updateInstrumentIndex(delta))
    }

    /** One bassoon. */
    inner class BassoonClone : CloneWithHands(this@Bassoon, 0.05f) {

        override val leftHands: List<Spatial> = Models.Reed.Bassoon.HandLeft.map { parent.context.model(it) }

        override val rightHands: List<Spatial> = Models.Reed.Bassoon.HandRight.map { parent.context.model(it) }

        private val bell = context.model(Models.Reed.Bassoon.Horn).also {
            geometry.attachChild(it)
            it.loc = BELL_POSITION
        }

        private val bellStretcher = StandardBellStretcher(0.25f, Axis.Y, bell)

        init {
            geometry.localTranslation.z = 5f
            geometry.attachChild(context.model(Models.Reed.Bassoon.Body))
            loadHands()
        }

        override fun adjustForPolyphony(delta: Duration) {
            root.localRotation = Quaternion().fromAngles(0f, rad(40 * indexForMoving()), 0f)
        }

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)
            bellStretcher.tick(currentNotePeriod, time)
        }
    }

    init {
        geometry.run {
            loc = v3(-58, 52, -38)
            localRotation = Quaternion().fromAngleAxis(rad(55.0), Vector3f.UNIT_Y)
                .mult(Quaternion().fromAngleAxis(rad(-20.0), Vector3f.UNIT_Z))
            setLocalScale(0.9f)
        }
    }

    private companion object {
        /** How far each further bassoon on stage stands from the last. */
        val MULTIPLE_INSTANCE_OFFSET: Vector3f = v3(-7, 0, -5)
    }
}
