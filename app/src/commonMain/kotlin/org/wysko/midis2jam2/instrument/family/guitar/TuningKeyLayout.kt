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

package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.resourceToString

/**
 * Where the tuning keys sit on an instrument's headstock, and the models that draw them; read from
 * `instrument/tuning/<instrument>.json`. See `docs/TUNING_KEYS.md` for how the models are made.
 *
 * Positions and rotations are written as Blender shows them (Z up, XYZ Euler in degrees), so they can be copied
 * straight from the model's scene; [blenderPosition] and [blenderRotation] convert them to the engine's Y-up space,
 * the same way the OBJ exporter converts the meshes.
 *
 * @property body The instrument's body, exported without its keys.
 * @property key One tuning key, with its pivot at the origin and turning about the model's +Y (Blender's +Z).
 * @property keyTexture The texture the key model (and the capo) is UV-mapped to, if not the body's.
 * @property keyRotation How the key model is turned within each key, as Blender XYZ Euler degrees, for a model that
 * was exported turned relative to its object in the scene.
 * @property keyScale How large each key is drawn, relative to the model.
 * @property capoZ How far the capo is moved out of the neck (toward the player is positive) from where `GuitarCapo.obj`
 * sits on the guitar, for an instrument whose strings lie higher or lower than the guitar's.
 * @property degreesPerSemitone How far a key turns for each semitone its string is tuned from standard.
 * @property keys Per string, lowest first, where its key sits.
 */
@Serializable
data class TuningKeyLayout(
    val body: String,
    val key: String,
    val keyTexture: String? = null,
    val keyRotation: List<Float> = listOf(0f, 0f, 0f),
    val keyScale: Float = 1f,
    val capoZ: Float = 0f,
    val degreesPerSemitone: Float = 20f,
    val keys: List<Key>,
) {
    /**
     * One key.
     *
     * @property position Where the key's pivot is, in Blender's coordinates.
     * @property rotation How the key is oriented, as Blender XYZ Euler degrees.
     * @property direction Which way tightening the string turns the key: `1` or `-1`.
     */
    @Serializable
    data class Key(val position: List<Float>, val rotation: List<Float> = listOf(0f, 0f, 0f), val direction: Float = 1f)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Blender's ([x], [y], [z]) in the engine's Y-up space: Blender's up (Z) is the engine's Y. */
        fun blenderPosition(x: Float, y: Float, z: Float): Vector3f = Vector3f(x, z, -y)

        /**
         * A Blender XYZ Euler rotation ([x], [y], [z] degrees; X applied first, then Y, then Z, about the scene's axes)
         * in the engine's Y-up space, where Blender's X, Y and Z axes are the engine's X, -Z and Y.
         */
        fun blenderRotation(x: Float, y: Float, z: Float): Quaternion {
            fun about(axis: Vector3f, degrees: Float) = Quaternion().fromAngleAxis(degrees * FastMath.DEG_TO_RAD, axis)
            return about(Vector3f.UNIT_Y, z).mult(about(Vector3f.UNIT_Z.negate(), y)).mult(about(Vector3f.UNIT_X, x))
        }

        /**
         * The body to draw for an instrument normally drawn with [model]: with its key art, the body exported without
         * keys ([body]), whatever the tuning; without it, the legacy rule of [droppedModel] when the lowest string is
         * [lowered].
         */
        fun bodyFor(model: String, droppedModel: String?, lowered: Boolean): String =
            forModel(model)?.body ?: if (lowered && droppedModel != null) droppedModel else model

        /** The key art for an instrument whose body is normally [model] (`Guitar.obj` reads `Guitar.json`). */
        fun forModel(model: String): TuningKeyLayout? = load(model.substringBeforeLast('.'))

        /** Reads `instrument/tuning/<instrument>.json`, or `null` while there is no key art for [instrument]. */
        fun load(instrument: String): TuningKeyLayout? =
            resourceToString("/instrument/tuning/$instrument.json").takeIf { it.isNotBlank() }?.let {
                json.decodeFromString(serializer(), it)
            }
    }
}
