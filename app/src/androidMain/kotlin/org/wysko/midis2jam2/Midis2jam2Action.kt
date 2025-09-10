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

package org.wysko.midis2jam2

sealed class Midis2jam2Action {
    data class MoveToCameraAngle(val cameraAngle: String) : Midis2jam2Action()
    data object SwitchToAutoCam : Midis2jam2Action()
    data object SwitchToSlideCam : Midis2jam2Action()
    data object SeekBackward : Midis2jam2Action()
    data object SeekForward : Midis2jam2Action()
    data object PlayPause : Midis2jam2Action()


    data class Pan(val panDeltaX: Float, val panDeltaY: Float) : Midis2jam2Action()
    data class Zoom(val zoomDelta: Float) : Midis2jam2Action()
    data class Orbit(val x: Float, val y: Float) : Midis2jam2Action()
}
