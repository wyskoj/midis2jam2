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

package org.wysko.midis2jam2.instrument.family.guitar

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.resourceToString

/**
 * A skin of the electric guitar.
 *
 * @property name A unique name for this skin.
 * @property file The texture file name.
 */
@Serializable
data class GuitarSkin(val name: String, val file: String) {
    companion object {
        private val skins = Json.decodeFromString<Collection<GuitarSkin>>(
            resourceToString("/instrument/textures/Guitar.json")
        )

        /**
         * Returns a [GuitarSkin] given its name.
         *
         * @param name The name of the desired [GuitarSkin].
         * @return The [GuitarSkin] that matches the given [name].
         */
        operator fun get(name: String): GuitarSkin = skins.first { it.name == name }
    }
}
