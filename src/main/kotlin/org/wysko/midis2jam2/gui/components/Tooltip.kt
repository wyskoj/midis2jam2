/*
 * Copyright (C) 2023 Jacob Wysko
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

package org.wysko.midis2jam2.gui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ToolTip(tooltip: String, target: @Composable () -> Unit) {
    var contentHeight by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var job: Job? by remember { mutableStateOf(null) }
    var isVisible by remember { mutableStateOf(false) }

    fun startShowing() {
        job?.cancel()
        job = scope.launch {
            delay(500L)
            isVisible = true
        }
    }

    fun hide() {
        job?.cancel()
        isVisible = false
    }


    Box(Modifier.onGloballyPositioned {
        contentHeight = it.size.height
    }.onPointerEvent(PointerEventType.Enter) {
        startShowing()
    }.pointerInput(Unit) {
        detectDown {
            hide()
        }
    }.onPointerEvent(PointerEventType.Exit) {
        hide()
    }) {
        target()
        if (contentHeight != 0) {
            Popup(alignment = Alignment.Center, offset = IntOffset(0, (-contentHeight + 8))) {
                AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
                    DrawTooltip(tooltip)
                }
            }
        }
    }
}

@Composable
private fun DrawTooltip(tooltip: String) {
    Surface(
        modifier = Modifier.height(24.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            tooltip,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

private suspend fun PointerInputScope.detectDown(onDown: (Offset) -> Unit) {
    while (true) {
        awaitPointerEventScope {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.find { it.changedToDown() }
            if (down != null) {
                onDown(down.position)
            }
        }
    }
}
