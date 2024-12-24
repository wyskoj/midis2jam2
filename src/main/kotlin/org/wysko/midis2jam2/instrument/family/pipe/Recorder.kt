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
package org.wysko.midis2jam2.instrument.family.pipe

import com.jme3.math.Quaternion
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.CloneWithPuffer
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

private val FINGERING_MANAGER = HandPositionFingeringManager.from(Recorder::class)

/** The Recorder. */
class Recorder(context: Midis2jam2, events: List<MidiEvent>) :
    InstrumentWithHands(context, events, RecorderClone::class, FINGERING_MANAGER) {

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(reversed = true)

    override fun adjustForMultipleInstances(delta: Duration) {
        root.setLocalTranslation(0f, 10f * updateInstrumentIndex(delta), 0f)
    }

    /**
     * A single Recorder.
     */
    inner class RecorderClone : CloneWithPuffer(this@Recorder, SteamPuffer.Texture.Pop, 1f) {
        override val leftHands: List<Spatial> = List(13) {
            parent.context.modelD("RecorderHandLeft$it.obj", "hands.bmp")
        }
        override val rightHands: List<Spatial> = List(11) {
            parent.context.modelD("RecorderHandRight$it.obj", "hands.bmp")
        }

        override fun adjustForPolyphony(delta: Duration) {
            root.localRotation = Quaternion().fromAngles(0f, rad((15f + indexForMoving() * 15).toDouble()), 0f)
        }

        init {
            loadHands()

            /* Align steam puffer */
            puffer.root.localRotation = Quaternion().fromAngles(floatArrayOf(0f, 0f, rad(-90.0)))
            puffer.root.setLocalTranslation(0f, -12.3f, 0f)

            geometry.attachChild(context.modelD("Recorder.obj", "Recorder.bmp"))
            animNode.setLocalTranslation(0f, 0f, 23f)
            highestLevel.localRotation = Quaternion().fromAngles(rad(45.5 - 90), 0f, 0f)
        }
    }

    init {
        geometry.setLocalTranslation(-7f, 35f, -25f)
    }
}
