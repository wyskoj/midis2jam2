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

package org.wysko.midis2jam2.manager.camera

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.wysko.midis2jam2.util.KClassAsString
import org.wysko.midis2jam2.util.resourceToString

@Serializable
data class CameraAngleCategory(
    val category: Int,
    val angles: List<CameraAngle>,
    val instrumentClass: KClassAsString? = null,
) {
    companion object {
        val categories: List<CameraAngleCategory> =
            Yaml.default.decodeFromString(resourceToString("/camera_angles.yaml"))
    }
}