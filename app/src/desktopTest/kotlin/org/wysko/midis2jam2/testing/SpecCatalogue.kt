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

package org.wysko.midis2jam2.testing

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable

/** One behavioural claim the product makes, as catalogued in `spec/behaviour.yaml`. */
@Serializable
data class SpecEntry(
    val id: String,
    val source: String,
    val platforms: List<String>,
    val claim: String,
    /** Why no automated test will ever cover this (out of reach of the suite). */
    val waived: String? = null,
    /** Why this is not covered *yet*. A shrinking backlog; must be empty when the suite is complete. */
    val pending: String? = null,
)

/** The behaviour catalogue, loaded from the test resources. */
object SpecCatalogue {

    private const val RESOURCE = "/spec/behaviour.yaml"

    val entries: List<SpecEntry> by lazy {
        val text = checkNotNull(SpecCatalogue::class.java.getResourceAsStream(RESOURCE)) {
            "Behaviour catalogue not found on the test classpath at $RESOURCE"
        }.bufferedReader().use { it.readText() }
        Yaml.default.decodeFromString(kotlinx.serialization.builtins.ListSerializer(SpecEntry.serializer()), text)
    }

    val byId: Map<String, SpecEntry> by lazy { entries.associateBy { it.id } }
}
