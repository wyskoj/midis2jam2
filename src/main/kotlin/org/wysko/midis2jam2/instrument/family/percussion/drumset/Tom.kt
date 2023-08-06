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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.Retexturable
import org.wysko.midis2jam2.instrument.family.percussion.RetextureType
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.Utils.rad

private val STICK_NODE_OFFSET = Vector3f(0f, 0f, 10f)

/** A Tom. */
class Tom(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>, pitch: TomPitch) :
    PercussionInstrument(context, hits), Retexturable {

    /** The drum. */
    private val drum = context.loadModel("DrumSet_Tom.obj", "DrumShell.bmp").apply {
        recoilNode.attachChild(this)
        localScale = Vector3f.UNIT_XYZ.clone().mult(pitch.scale)
    }

    /** The stick. */
    private val stick: Striker = Striker(context, hits, StickType.DRUMSET_STICK).apply {
        setParent(recoilNode)
    }

    init {
        // Move and rotate tom based on its pitch
        instrumentNode.localTranslation = pitch.location()
        instrumentNode.localRotation = pitch.rotation()

        // Move stick so that it strikes on the drum
        stick.node.move(STICK_NODE_OFFSET)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val result = stick.tick(time, delta)
        recoilDrum(
            drum = recoilNode,
            velocity = result.strike?.velocity ?: 0,
            delta = delta
        )
    }

    override fun drum(): Spatial = drum
    override fun retextureType(): RetextureType = RetextureType.OTHER
}

/** The pitch of the tom. */
@Suppress("kotlin:S6218", "ArrayInDataClass")
@Serializable
data class TomPitch(
    /**
     * The name of the pitch.
     */
    val name: String,
    /**
     * The scale of the pitch.
     */
    val scale: Float,
    private val location: Array<Float>,
    private val rotation: Array<Float>
) {
    /** Returns the location of this as a [Vector3f]. */
    fun location(): Vector3f {
        return Vector3f(location[0], location[1], location[2])
    }

    /** Returns the rotation of this as a [Quaternion]. */
    fun rotation(): Quaternion {
        return Quaternion().fromAngles(
            rad(rotation[0]),
            rad(rotation[1]),
            rad(rotation[2])
        )
    }

    companion object {
        private val pitches =
            Json.decodeFromString<Array<TomPitch>>(Utils.resourceToString("/instrument/alignment/Tom.json"))

        /** Retrieves a [TomPitch] based on its name from the resource file. */
        operator fun get(name: String): TomPitch = pitches.first { it.name == name }
    }
}
