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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun rememberLoadingController(): LoadingController = remember { LoadingController() }

data class LoadingState(
    val ready: Boolean = false,
    val showLoading: Boolean = true,
    val loadProgress: String = "",
    val loadProgressDecimal: Float = 0f,
)

class LoadingController {
    private val _state = MutableStateFlow(LoadingState())
    val state: StateFlow<LoadingState> get() = _state

    val animatedProgress: State<Float>
        @Composable
        get() {
            val targetValue = _state.collectAsState().value.loadProgressDecimal
            return animateFloatAsState(
                targetValue,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            )
        }

    fun setReady(ready: Boolean) {
        _state.value = _state.value.copy(ready = ready)
    }

    fun setShowLoading(show: Boolean) {
        _state.value = _state.value.copy(showLoading = show)
    }

    fun setLoadProgress(progress: String) {
        _state.value = _state.value.copy(loadProgress = progress)
    }

    fun setLoadProgressDecimal(progress: Float) {
        _state.value = _state.value.copy(loadProgressDecimal = progress)
    }
}