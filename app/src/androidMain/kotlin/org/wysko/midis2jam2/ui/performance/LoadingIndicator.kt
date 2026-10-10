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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.loading_building_band
import midis2jam2.app.generated.resources.loading_reading_midi
import midis2jam2.app.generated.resources.loading_soundbank
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.starter.LoadingStage

/**
 * What's shown over the performance while it loads: the song's [title], a progress bar, and what's being done.
 *
 * The bar is indeterminate until the stage being worked on can say how far along it is.
 */
@Composable
fun LoadingIndicator(
    visible: Boolean,
    title: String,
    state: LoadingState,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(320.dp).padding(16.dp),
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val progress = state.progress
                if (progress == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    val animatedProgress by animateFloatAsState(
                        progress,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    )
                    LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
                }
                Text(
                    state.stage?.let { stringResource(it.label) } ?: "",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private val LoadingStage.label: StringResource
    get() = when (this) {
        LoadingStage.ReadingMidi -> Res.string.loading_reading_midi
        LoadingStage.LoadingSoundbank -> Res.string.loading_soundbank
        LoadingStage.BuildingBand -> Res.string.loading_building_band
    }
