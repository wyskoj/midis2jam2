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

package org.wysko.midis2jam2.gui

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.Utils.resourceToString

/**
 * midis2jam2 makes use of open-source libraries. We attribute the authorship of these libraries to their respective
 * owners by use of this data structure.
 */
@Serializable
data class DependenciesJsonObject(
    /** The list of dependencies. */
    val dependencies: MutableList<Dependency>
)

/**
 * A single dependency.
 */
@Serializable
data class Dependency(
    /** The name of the library. */
    val name: String,
    /** The file of the library. */
    val file: String,
    /** The list of licenses of the library. */
    val licenses: List<License>
)

/**
 * A license.
 */
@Serializable
data class License(
    /** The name of the license. */
    val name: String,

    /** A URL to the content of the license. It may be null if there is no license. */
    val url: String?
)

/** Loads the dependency attribution data. */
fun loadDependencies(): List<Dependency> =
    Json.decodeFromString<DependenciesJsonObject>(resourceToString("/dependency-license.json"))
        .also { (dependencies1) ->
            dependencies1.sortBy { it.name }
        }.dependencies