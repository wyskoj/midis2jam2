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

package org.wysko.midis2jam2.ui.common.transition

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import cafe.adriel.voyager.transitions.ScreenTransitionContent

@Composable
fun SlideAndFadeTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: ScreenTransitionContent = { it.Content() }
) {
    val finiteAnimationSpec =
        spring<Float>(stiffness = Spring.StiffnessMediumLow)

    val animationSpec = spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        ScreenTransition(
            navigator = navigator,
            modifier = modifier,
            content = content,
            transition = {
                val (initialOffset, targetOffset) = when (navigator.lastEvent) {
                    StackEvent.Pop -> ({ size: Int -> -128 }) to ({ size: Int -> 128 })
                    else -> ({ size: Int -> 128 }) to ({ size: Int -> -128 })
                }
                slideInHorizontally(
                    animationSpec,
                    initialOffset
                ) + fadeIn(finiteAnimationSpec) togetherWith
                        slideOutHorizontally(animationSpec, targetOffset) + fadeOut(
                    finiteAnimationSpec
                )
            }
        )
    }
}