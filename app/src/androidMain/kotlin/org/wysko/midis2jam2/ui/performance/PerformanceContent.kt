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

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import org.wysko.midis2jam2.CompatLibrary
import org.wysko.midis2jam2.Midis2jam2Action
import org.wysko.midis2jam2.R
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.starter.Midis2jam2Harness
import org.wysko.midis2jam2.util.findActivity
import org.wysko.midis2jam2.util.rememberIsInPipMode
import org.wysko.midis2jam2.util.systemBackLegacy
import kotlin.time.Duration.Companion.milliseconds

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
    var pipSourceRectHint by remember { mutableStateOf<Rect?>(null) }
    val isPlaybackPlaying = remember { MutableStateFlow(true) }
    val currentIsPlaybackPlaying by isPlaybackPlaying.collectAsState()

    // drive the loading exit delay
    LaunchedEffect(loadingState.ready) {
        if (loadingState.ready) {
            scope.launch {
                delay(750.milliseconds)
                loadingController.setShowLoading(false)
            }
        }
    }

    // harness
    val harness = remember {
        Midis2jam2Harness(context, onFinish).apply {
            registerProgressListener(listener)
            registerPlaybackStateListener { isPlaybackPlaying.value = it }
        }
    }
    val isAutoCamActive by harness.isAutoCamActive.collectAsState()
    val isSlideCamActive by harness.isSlideCamActive.collectAsState()
    val isPlaybackPlayingState by rememberUpdatedState(currentIsPlaybackPlaying)

    val callAction: (Midis2jam2Action) -> Unit = { action ->
        harness.callAction(action)
    }

    fun updatePictureInPictureParams(sourceRectHint: Rect?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val activity = context.findActivity()
        val builder = PictureInPictureParams.Builder()
            .setActions(
                listOf(
                    context.pipRemoteAction(
                        titleRes = R.string.pip_rewind_10_seconds,
                        iconResId = R.drawable.replay_10,
                        action = PIP_ACTION_SEEK_BACKWARD,
                        requestCode = 1
                    ),
                    context.pipRemoteAction(
                        titleRes = if (isPlaybackPlayingState) R.string.pip_pause else R.string.pip_play,
                        iconResId = if (isPlaybackPlayingState) R.drawable.pause else R.drawable.play_arrow,
                        action = PIP_ACTION_PLAY_PAUSE,
                        requestCode = 2
                    ),
                    context.pipRemoteAction(
                        titleRes = R.string.pip_fast_forward_10_seconds,
                        iconResId = R.drawable.forward_10,
                        action = PIP_ACTION_SEEK_FORWARD,
                        requestCode = 3
                    )
                )
            )
        sourceRectHint?.let(builder::setSourceRectHint)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(false)
        }
        activity.setPictureInPictureParams(builder.build())
    }

    val pipActionReceiver = remember(harness) {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    PIP_ACTION_PLAY_PAUSE -> callAction(Midis2jam2Action.PlayPause)
                    PIP_ACTION_SEEK_BACKWARD -> callAction(Midis2jam2Action.SeekBackward)
                    PIP_ACTION_SEEK_FORWARD -> callAction(Midis2jam2Action.SeekForward)
                }
            }
        }
    }

    DisposableEffect(harness) {
        onDispose {
            harness.shutdown()
        }
    }

    DisposableEffect(context, pipActionReceiver) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter().apply {
                addAction(PIP_ACTION_PLAY_PAUSE)
                addAction(PIP_ACTION_SEEK_BACKWARD)
                addAction(PIP_ACTION_SEEK_FORWARD)
            }
            ContextCompat.registerReceiver(
                context,
                pipActionReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.unregisterReceiver(pipActionReceiver)
            }
        }
    }

    // back handling at activity level
    BackHandler {
        harness.shutdown()
        applicationService.onApplicationFinished()
    }

    val inPipMode = rememberIsInPipMode()
    LaunchedEffect(currentIsPlaybackPlaying) {
        updatePictureInPictureParams(pipSourceRectHint)
    }

    val pipModifier = Modifier.onGloballyPositioned { coordinates ->
        val sourceRectHint = coordinates.boundsInWindow().toAndroidRectF().toRect()
        pipSourceRectHint = sourceRectHint
        updatePictureInPictureParams(sourceRectHint)
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
                    ControlsCard(isAutoCamActive, isSlideCamActive, callAction)
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

private fun Context.pipRemoteAction(
    titleRes: Int,
    @DrawableRes iconResId: Int,
    action: String,
    requestCode: Int,
): RemoteAction {
    val intent = Intent(action).setPackage(packageName)
    val pendingIntent = PendingIntent.getBroadcast(
        this,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val title = getString(titleRes)
    val icon = Icon.createWithResource(this, iconResId)
    return RemoteAction(icon, title, title, pendingIntent)
}

private const val PIP_ACTION_PLAY_PAUSE = "org.wysko.midis2jam2.PIP_ACTION_PLAY_PAUSE"
private const val PIP_ACTION_SEEK_BACKWARD = "org.wysko.midis2jam2.PIP_ACTION_SEEK_BACKWARD"
private const val PIP_ACTION_SEEK_FORWARD = "org.wysko.midis2jam2.PIP_ACTION_SEEK_FORWARD"
