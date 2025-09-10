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

import android.app.PictureInPictureParams
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.CompatLibrary
import org.wysko.midis2jam2.Midis2jam2Action
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.starter.Midis2jam2Harness
import org.wysko.midis2jam2.util.findActivity
import org.wysko.midis2jam2.util.rememberIsInPipMode
import org.wysko.midis2jam2.util.systemBackLegacy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerformanceContent(
    applicationService: ApplicationService,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingController = rememberLoadingController()
    val loadingState by loadingController.state.collectAsState()
    val animatedProgress = loadingController.animatedProgress
    val listener = remember { LoadingListener(loadingController) }

    var showControls by rememberSaveable { mutableStateOf(false) }

    // drive the loading exit delay
    LaunchedEffect(loadingState.ready) {
        if (loadingState.ready) {
            scope.launch {
                delay(750)
                loadingController.setShowLoading(false)
            }
        }
    }

    // harness
    val harness = remember { Midis2jam2Harness(context, onFinish).apply { registerProgressListener(listener) } }
    val isAutoCamActive by harness.isAutoCamActive.collectAsState()
    val isSlideCamActive by harness.isSlideCamActive.collectAsState()

    // back handling at activity level
    BackHandler {
        harness.stop()
        applicationService.onApplicationFinished()
    }

    val inPipMode = rememberIsInPipMode()

    val pipModifier = Modifier.onGloballyPositioned {
        val builder = PictureInPictureParams.Builder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
        }
        context.findActivity().setPictureInPictureParams(builder.build())
    }


    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .then(pipModifier),
            factory = {
                harness.view.apply {
                    if (CompatLibrary.requiresPerformanceViewSystemBack) {
                        systemBackLegacy(harness, applicationService, onFinish)
                    }
                }
            }
        )

        if (loadingState.ready) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .multiTouchGestures(
                        onTap = { showControls = !showControls },
                        onPan = { harness.callAction(Midis2jam2Action.Pan(it.x, it.y)) },
                        onZoom = { harness.callAction(Midis2jam2Action.Zoom(1 - it)) },
                        onDrag = { harness.callAction(Midis2jam2Action.Orbit(it.x, it.y)) }
                    )
            ) {
                AnimatedVisibility(
                    visible = showControls && !inPipMode,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    ControlsCard(isAutoCamActive, isSlideCamActive, harness::callAction)
                }
            }
        }

        LoadingIndicator(
            showLoading = loadingState.showLoading,
            ready = loadingState.ready,
            animatedProgress = animatedProgress.value,
            loadProgress = loadingState.loadProgress
        )
    }
}
