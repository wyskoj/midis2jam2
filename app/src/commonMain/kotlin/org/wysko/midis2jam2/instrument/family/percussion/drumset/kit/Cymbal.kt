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
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSetInstrument
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.resourceToString
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.MaterialType
import org.wysko.midis2jam2.world.model
import kotlin.time.Duration

private val STICK_POSITION_MAP = buildMap {
    put("splash", v3(0, 2, 14))
}
private val DEFAULT_STICK_POSITION = v3(0, 2, 18)

/**
 * A crash, splash, ride, or china cymbal.
 */
open class Cymbal(
    context: Midis2jam2,
    hits: List<NoteEvent.NoteOn>,
    type: CymbalType,
    style: Style = Style.Standard
) : DrumSetInstrument(context, hits) {
    /**
     * The stick that strikes the cymbal.
     */
    protected val stick: Striker = Striker(context, hits, StickType.DRUM_SET_STICK).apply {
        setParent(geometry)
        node.move(
            STICK_POSITION_MAP.getOrDefault(
                type.name,
                DEFAULT_STICK_POSITION
            )
        )
        offsetStick {
            it.rot = v3(-20, 0, 0) // Angles stick down slightly
        }
    }

    private val model = with(geometry) {
        +context.model(type.model, style.texture, style.materialType).apply {
            scale(type.size)
        }
    }

    /** The cymbal animator. */
    protected val cymbalAnimator: CymbalAnimator =
        CymbalAnimator(model, type.amplitude, type.wobbleSpeed, type.dampening)

    init {
        geometry.localTranslation = type.location()
        geometry.localRotation = type.rotation()
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        stick.tick(time, delta).strike?.let { cymbalAnimator.strike() }
        cymbalAnimator.tick(delta)
    }

    /**
     * The style of the cymbal.
     *
     * @property texture The texture of the cymbal.
     * @property materialType The material type of the cymbal.
     */
    sealed class Style(val texture: String, val materialType: MaterialType) {
        /**
         * The standard style of cymbal.
         */
        data object Standard : Style("CymbalSkinSphereMap.bmp", MaterialType.Reflective)

        /**
         * The electronic style of cymbal.
         */
        data object Electronic : Style("RubberFoot.bmp", MaterialType.Diffuse)
    }
}


/**
 * Defines properties about a type of cymbal.
 *
 * @property name The name of the cymbal.
 * @property model The name of the model file.
 * @property size The size (or scale) of the cymbal.
 * @property amplitude The total rotational amplitude when the cymbal is struck.
 * @property wobbleSpeed The speed at which the cymbal should wobble after it is struck.
 */
@Suppress("kotlin:S6218", "ArrayInDataClass")
@Serializable
data class CymbalType(
    val name: String,
    val model: String,
    val size: Float,
    val amplitude: Double,
    val wobbleSpeed: Double,
    private val location: Array<Float>,
    private val rotation: Array<Float>,
) {
    /**
     * The rate at which the wobble fades out to rest.
     */
    val dampening: Double = 1.5

    /**
     * Returns the location of this as a [Vector3f].
     */
    fun location(): Vector3f = Vector3f(location[0], location[1], location[2])

    /**
     * Returns the rotation of this as a [Quaternion].
     */
    fun rotation(): Quaternion = Quaternion().fromAngles(rad(rotation[0]), rad(rotation[1]), rad(rotation[2]))

    companion object {
        private val values =
            Json.decodeFromString<Array<CymbalType>>(resourceToString("/instrument/alignment/Cymbal.json"))

        /** Retrieves a [CymbalType] given its [name]. */
        operator fun get(name: String): CymbalType = values.first { it.name == name }
    }
}
