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
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.Retexturable
import org.wysko.midis2jam2.instrument.family.percussion.RetextureType
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

/** A Tom. */
class Tom(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>, pitch: TomPitch) :
    SingleStickInstrument(context, hits), Retexturable {

    /** The drum. */
    var drum: Spatial

    override fun tick(time: Double, delta: Float) {
        val handleStick = handleStick(time, delta, hits)
        recoilDrum(
            drum,
            handleStick.justStruck(),
            if (handleStick.justStruck()) (handleStick.strike ?: return).velocity else 0,
            delta
        )
    }

    /** The pitch of the tom. */
    enum class TomPitch(
        /**
         * The size of the Tom.
         */
        val scale: Vector3f,
        /**
         * The location of the Tom.
         */
        val location: Vector3f,
        /**
         * The rotation of the Tom.
         */
        val rotation: Quaternion
    ) {
        /** The Low floor tom. */
        LOW_FLOOR(
            Vector3f(1.5f, 1.5f, 1.5f),
            Vector3f(20f, 20f, -60f),
            Quaternion().fromAngles(rad(-2.0), rad(180.0), rad(-10.0))
        ),

        /** The High floor tom. */
        HIGH_FLOOR(
            Vector3f(1.4f, 1.4f, 1.4f),
            Vector3f(17f, 21f, -75f),
            Quaternion().fromAngles(rad(-5.0), rad(180.0), rad(-15.0))
        ),

        /** The Low tom. */
        LOW(
            Vector3f(1.2f, 1.2f, 1.2f),
            Vector3f(10f, 29f, -82f),
            Quaternion().fromAngles(rad(60.0), rad(-30.0), 0f)
        ),

        /** The Low mid tom. */
        LOW_MID(
            Vector3f(1f, 1f, 1f),
            Vector3f(0f, 32f, -85f),
            Quaternion().fromAngles(rad(60.0), 0f, 0f)
        ),

        /** The High mid tom. */
        HIGH_MID(
            Vector3f(0.8f, 0.8f, 0.8f),
            Vector3f(-9f, 31f, -82f),
            Quaternion().fromAngles(rad(60.0), rad(20.0), 0f)
        ),

        /** The High tom. */
        HIGH(
            Vector3f(0.6f, 0.6f, 0.6f),
            Vector3f(-15f, 29f, -78f),
            Quaternion().fromAngles(rad(50.0), rad(40.0), 0f)
        );
    }

    init {
        /* Load the tom */
        drum = context.loadModel("DrumSet_Tom.obj", "DrumShell.bmp")

        /* Attach to nodes */
        recoilNode.attachChild(drum)
        recoilNode.attachChild(stickNode)
        highLevelNode.attachChild(recoilNode)

        /* Set tom pitch properties */
        drum.localScale = pitch.scale
        highLevelNode.localTranslation = pitch.location
        highLevelNode.localRotation = pitch.rotation
        if (pitch == TomPitch.HIGH_FLOOR || pitch == TomPitch.LOW_FLOOR) {
            stickNode.localRotation = Quaternion().fromAngles(0f, rad(80.0), 0f)
            stickNode.setLocalTranslation(10f, 0f, 0f)
        } else {
            stickNode.setLocalTranslation(0f, 0f, 10f)
        }
        stick.localRotation = Quaternion().fromAngles(rad(Stick.MAX_ANGLE), 0f, 0f)
    }

    override fun drum(): Spatial = drum
    override fun retextureType(): RetextureType = RetextureType.OTHER
}