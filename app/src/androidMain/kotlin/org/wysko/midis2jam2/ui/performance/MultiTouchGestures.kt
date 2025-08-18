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

package org.wysko.midis2jam2.ui.performance

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import org.wysko.midis2jam2.util.detectGestures

fun Modifier.multiTouchGestures(
    onTap: () -> Unit,
    onPan: (offset: Offset) -> Unit,
    onZoom: (zoom: Float) -> Unit,
    onDrag: (offset: Offset) -> Unit,
): Modifier {
    return pointerInput(Unit) {
        var fingerCount = 0
        detectGestures(
            onTap = {
                onTap()
            },
            onDrag = { ch, drag ->
                if (fingerCount == 1) {
                    onDrag(drag)
                }
            },
            onTransform = { _, pan, zoom, _ ->
                if (fingerCount == 2) {
                    if (pan.x != 0f || pan.y != 0f) {
                        onPan(pan)
                    }
                    if (zoom != 1f) {
                        onZoom(zoom)
                    }
                }
            },
            onFingerCountChanged = { fingerCount = it }
        )
    }
}
