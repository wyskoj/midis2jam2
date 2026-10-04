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

package org.wysko.midis2jam2.ui.settings

import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** A face of the cube. [index] is its position in the saved list of textures. */
internal enum class CubeMapFace(val index: Int, val label: StringResource) {
    North(0, Res.string.settings_background_direction_north),
    East(1, Res.string.settings_background_direction_east),
    South(2, Res.string.settings_background_direction_south),
    West(3, Res.string.settings_background_direction_west),
    Up(4, Res.string.settings_background_direction_up),
    Down(5, Res.string.settings_background_direction_down),
}
