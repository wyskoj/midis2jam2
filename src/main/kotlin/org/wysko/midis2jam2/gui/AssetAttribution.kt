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

package org.wysko.midis2jam2.gui

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.Utils.resourceToString

/** midis2jam2 may make use of assets released under free licenses; we attribute them with this data structure. */
@Serializable
data class AssetAttribution(
    /** The name of the asset. */
    val name: String,

    /** The author of the asset. */
    val author: String,

    /** The license of the asset. */
    val license: String,

    /** The URL of the asset's license. */
    val url: String,

    /** Any extra information about the use of the asset. */
    val extra: String
) {
    companion object {
        /** Load the attribution data from the `attributions.json` file. They are sorted by the name of the asset. */
        fun loadAttributions(): Array<AssetAttribution> =
            Json.decodeFromString<Array<AssetAttribution>>(resourceToString("/attributions.json"))
                .also { attributions ->
                    attributions.sortBy { it.name }
                }
    }
}
