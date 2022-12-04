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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.Utils.rad

internal val STICK_POSITION = Vector3f(0f, 2f, 18f)
private val STICK_PIVOT_OFFSET = Vector3f(0f, 0f, 0f)

/** Cymbals are represented with this class, excluding the [HiHat]. */
open class Cymbal(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>, type: CymbalType) :
    PercussionInstrument(context, hits) {

    /** The stick. */
    protected val stick: Striker = Striker(context, hits, StickType.DRUMSET_STICK).apply {
        setParent(instrumentNode)
        node.move(STICK_POSITION) // So the stick hits the edge of cymbal
        offsetStick {
            it.move(STICK_PIVOT_OFFSET) // Changes rotation pivot
            it.rotate(rad(-20.0), 0f, 0f) // Angles stick down slightly
        }
    }
    private val cymbal = context.loadModel(type.model, "CymbalSkinSphereMap.bmp", 0.7f).apply {
        instrumentNode.attachChild(this)
        this.scale(type.size)
    }

    /** The cymbal animator. */
    protected val cymbalAnimator: CymbalAnimator =
        CymbalAnimator(cymbal, type.amplitude, type.wobbleSpeed, type.dampening)

    init {
        // Position cymbal
        instrumentNode.localTranslation = type.location()
        instrumentNode.localRotation = type.rotation()
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        stick.tick(time, delta).strike?.let {
            cymbalAnimator.strike()
        }
        cymbalAnimator.tick(delta)
    }
}

/**
 * Defines properties about a type of cymbal.
 */
@Suppress("kotlin:S6218", "ArrayInDataClass")
@Serializable
data class CymbalType(
    /** The name of the cymbal. */
    val name: String,
    /** The name of its model. */
    val model: String,
    private val location: Array<Float>,
    private val rotation: Array<Float>,
    /** The size, or scale factor, of the cymbal. */
    val size: Float,
    /** The total rotational amplitude when the cymbal is struck. */
    val amplitude: Double,
    /** The speed at which the cymbal should wobble after it is struck. */
    val wobbleSpeed: Double
) {
    /** The rate at which the wobble fades out to rest. */
    val dampening: Double = 1.5

    /** Returns the location of this as a [Vector3f]. */
    fun location(): Vector3f = Vector3f(location[0], location[1], location[2])

    /** Returns the rotation of this as a [Quaternion]. */
    fun rotation(): Quaternion = Quaternion().fromAngles(rad(rotation[0]), rad(rotation[1]), rad(rotation[2]))

    companion object {
        private val values =
            Json.decodeFromString<Array<CymbalType>>(Utils.resourceToString("/instrument/alignment/Cymbal.json"))

        /** Retrieves a [CymbalType] given its [name]. */
        operator fun get(name: String): CymbalType = values.first { it.name == name }
    }
}
