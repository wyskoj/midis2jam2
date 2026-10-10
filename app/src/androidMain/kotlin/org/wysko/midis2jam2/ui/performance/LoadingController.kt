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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wysko.midis2jam2.starter.LoadingStage
import org.wysko.midis2jam2.starter.ProgressListener

@Composable
fun rememberLoadingController(): LoadingController = remember { LoadingController() }

/**
 * How far a performance has got with loading.
 *
 * @property stage The stage loading is in, or `null` before it has started.
 * @property progress How much of the stage is done, or `null` while that isn't known.
 * @property ready Whether the performance has loaded.
 */
data class LoadingState(
    val stage: LoadingStage? = null,
    val progress: Float? = null,
    val ready: Boolean = false,
)

/** Collects loading progress, which arrives from the loading and render threads, into state for the UI. */
class LoadingController : ProgressListener {
    private val _state = MutableStateFlow(LoadingState())
    val state: StateFlow<LoadingState> get() = _state

    override fun onLoadingStage(stage: LoadingStage) {
        _state.update { it.copy(stage = stage, progress = null) }
    }

    override fun onLoadingProgress(progress: Float) {
        _state.update { it.copy(progress = progress) }
    }

    override fun onReady() {
        _state.update { it.copy(ready = true) }
    }
}
