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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.resourceToString

/**
 * Where the tuning keys sit on an instrument's headstock, and the models that draw them; read from
 * `instrument/tuning/<instrument>.json`. See `docs/TUNING_KEYS.md` for how the models are made.
 *
 * @property body The instrument's body, exported without its keys.
 * @property key One tuning key, with its pivot at the origin and turning about +Y.
 * @property degreesPerSemitone How far a key turns for each semitone its string is tuned from standard.
 * @property keys Per string, lowest first, where its key sits.
 */
@Serializable
data class TuningKeyLayout(
    val body: String,
    val key: String,
    val degreesPerSemitone: Float = 90f,
    val keys: List<Key>,
) {
    /**
     * One key.
     *
     * @property position Where the key's pivot is, in the body's model space.
     * @property rotation How the key is oriented, as Euler angles in degrees.
     * @property direction Which way tightening the string turns the key: `1` or `-1`.
     */
    @Serializable
    data class Key(val position: List<Float>, val rotation: List<Float> = listOf(0f, 0f, 0f), val direction: Float = 1f)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * The body to draw for an instrument normally drawn with [model]: with its key art, the body exported without
         * keys ([body]), whatever the tuning; without it, the legacy rule of [droppedModel] when the lowest string is
         * [lowered].
         */
        fun bodyFor(model: String, droppedModel: String, lowered: Boolean): String =
            forModel(model)?.body ?: if (lowered) droppedModel else model

        /** The key art for an instrument whose body is normally [model] (`Guitar.obj` reads `Guitar.json`). */
        fun forModel(model: String): TuningKeyLayout? = load(model.substringBeforeLast('.'))

        /** Reads `instrument/tuning/<instrument>.json`, or `null` while there is no key art for [instrument]. */
        fun load(instrument: String): TuningKeyLayout? =
            resourceToString("/instrument/tuning/$instrument.json").takeIf { it.isNotBlank() }?.let {
                json.decodeFromString(serializer(), it)
            }
    }
}
