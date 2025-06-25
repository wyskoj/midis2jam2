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

package org.wysko.midis2jam2.starter.configuration

import kotlinx.serialization.Serializable

/**
 * Represents a resolution for display and rendering.
 */
@Serializable
sealed class Resolution {

    /**
     * Represents a custom resolution for the application.
     *
     * @property width The width of the resolution.
     * @property height The height of the resolution.
     */
    @Serializable
    data class CustomResolution(val width: Int, val height: Int) : Resolution() {
        override fun toString(): String = "${width}x$height"
    }
}

