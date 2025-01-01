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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.kit

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSetInstrument
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

private val STICK_NODE_OFFSET = Vector3f(0f, 0f, 10f)

/** A Tom. */
class Tom(context: Midis2jam2, hits: MutableList<NoteEvent.NoteOn>, pitch: TomPitch, style: ShellStyle) :
    DrumSetInstrument(context, hits) {
    /** The drum. */
    private val drum =
        context.modelD(style.tomModel, style.shellTexture).apply {
            recoilNode.attachChild(this)
            localScale = Vector3f.UNIT_XYZ.clone().mult(pitch.scale)
        }

    /** The stick. */
    private val stick: Striker =
        Striker(context, hits, StickType.DRUM_SET_STICK).apply {
            setParent(recoilNode)
        }

    init {
        // Move and rotate tom based on its pitch
        geometry.localTranslation = pitch.location()
        geometry.localRotation = pitch.rotation()

        // Move stick so that it strikes on the drum
        stick.node.move(STICK_NODE_OFFSET)
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        val result = stick.tick(time, delta)
        recoilDrum(
            drum = recoilNode,
            velocity = result.strike?.velocity ?: 0,
            delta = delta,
        )
    }
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
    private val rotation: Array<Float>,
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
            rad(rotation[2]),
        )
    }

    companion object {
        private val pitches =
            Json.decodeFromString<Array<TomPitch>>(Utils.resourceToString("/instrument/alignment/Tom.json"))

        /** Retrieves a [TomPitch] based on its name from the resource file. */
        operator fun get(name: String): TomPitch = pitches.first { it.name == name }
    }
}
