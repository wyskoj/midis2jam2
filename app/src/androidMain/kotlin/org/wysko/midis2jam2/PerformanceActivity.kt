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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.WorldOneRegular
import midis2jam2.app.generated.resources.forward_10
import midis2jam2.app.generated.resources.play_pause
import midis2jam2.app.generated.resources.replay_10
import midis2jam2.app.generated.resources.videocam
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.starter.Midis2jam2Harness
import org.wysko.midis2jam2.starter.ProgressListener
import org.wysko.midis2jam2.ui.common.material.AppTheme
import org.wysko.midis2jam2.util.logger
import kotlin.math.PI
import kotlin.math.abs

class PerformanceActivity : ComponentActivity(), KoinComponent {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val applicationService: ApplicationService by inject()

        setContent {
            var loadProgress by remember { mutableStateOf("") }
            var loadProgressDecimal by remember { mutableFloatStateOf(0f) }
            val animatedProgress by animateFloatAsState(
                targetValue = loadProgressDecimal,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
            )
            val scope = rememberCoroutineScope()

            var ready by rememberSaveable { mutableStateOf(false) }
            var showControls by rememberSaveable { mutableStateOf(false) }
            var showLoading by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(ready) {
                if (ready) {
                    scope.launch {
                        delay(750)
                        showLoading = false
                    }
                }
            }

            val listener = object : ProgressListener {
                override fun onReady() {
                    ready = true
                }

                override fun onLoadingAsset(assetName: String) {
                    loadProgress = assetName
                }

                override fun onLoadingProgress(progress: Float) {
                    loadProgressDecimal = progress
                }
            }

            val harness = remember {
                Midis2jam2Harness(
                    context = this,
                    onFinish = {
                        applicationService.onApplicationFinished()
                        finish()
                    }
                ).apply {
                    registerProgressListener(listener)
                }
            }
            val isAutoCamActive = harness.isAutoCamActive.collectAsState()
            val isSlideCamActive = harness.isSlideCamActive.collectAsState()

            onBackPressedDispatcher.addCallback {
                logger().debug("killing harness on back press")
                harness.stop()
                applicationService.onApplicationFinished()
            }

            AppTheme {
                Scaffold {
                    Box {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.TopCenter),
                            factory = {
                                harness.view.apply {
                                    if (CompatLibrary.requiresPerformanceViewSystemBack) {
                                        isFocusableInTouchMode = true
                                        requestFocus()
                                        setOnKeyListener { _, keyCode, event ->
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                Log.d("BackPress", "View-level back press")
                                                harness.stop()
                                                applicationService.onApplicationFinished()
                                                finish()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        if (ready) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                                    .padding(8.dp)
                                    .pointerInput(Unit) {
                                        var fingerCount = 0

                                        detectGestures(
                                            onTap = {
                                                showControls = !showControls
                                            },
                                            onDrag = { change, dragAmount ->
                                                if (fingerCount == 1) {
                                                    // One-finger drag = orbit
                                                    harness.callAction(
                                                        Midis2jam2Action.Orbit(
                                                            dragAmount.x,
                                                            dragAmount.y
                                                        )
                                                    )
                                                }
                                            },
                                            onTransform = { centroid, pan, zoom, _ ->
                                                if (fingerCount == 2) {
                                                    // Two-finger drag = pan
                                                    if (pan.x != 0f || pan.y != 0f) {
                                                        harness.callAction(
                                                            Midis2jam2Action.Pan(
                                                                pan.x,
                                                                pan.y
                                                            )
                                                        )
                                                    }

                                                    // Two-finger pinch = zoom
                                                    if (zoom != 1f) {
                                                        harness.callAction(Midis2jam2Action.Zoom(1 - zoom))
                                                    }
                                                }
                                            },
                                            onFingerCountChanged = { count ->
                                                fingerCount = count
                                            }
                                        )
                                    }
                            ) {
                                AnimatedVisibility(
                                    visible = showControls,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    OnScreenControls(
                                        isAutoCamActive.value,
                                        isSlideCamActive.value,
                                        harness::callAction
                                    )
                                }
                            }
                        }
                        Box(Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                showLoading,
                                Modifier.align(Alignment.Center),
                                enter = expandVertically(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { if (ready) 1f else animatedProgress },
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(loadProgress, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        WindowCompat.getInsetsController(window, window.decorView).run {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    @Composable
    private fun OnScreenControls(
        isAutoCamActive: Boolean,
        isSlideCamActive: Boolean,
        callAction: (Midis2jam2Action) -> Unit,
    ) {
        val worldOneRegular = FontFamily(Font(Res.font.WorldOneRegular))
        var showCameraControls by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .padding(8.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(showCameraControls) {
                Card {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 1..3) {
                                FilledTonalIconButton(onClick = {
                                    callAction(Midis2jam2Action.MoveToCameraAngle("cam$i"))
                                }) {
                                    Text(text = "$i", fontFamily = worldOneRegular)
                                }
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    callAction(Midis2jam2Action.SwitchToAutoCam)
                                },
                                colors = if (!isAutoCamActive) {
                                    IconButtonDefaults.filledTonalIconButtonColors()
                                } else {
                                    IconButtonDefaults.filledIconButtonColors()
                                }
                            ) {
                                Text(text = "A", fontFamily = worldOneRegular)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 4..6) {
                                FilledTonalIconButton(
                                    onClick = {
                                        callAction(Midis2jam2Action.MoveToCameraAngle("cam$i"))
                                    }
                                ) {
                                    Text(text = "$i", fontFamily = worldOneRegular)
                                }
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    callAction(Midis2jam2Action.SwitchToSlideCam)
                                },
                                colors = if (!isSlideCamActive) {
                                    IconButtonDefaults.filledTonalIconButtonColors()
                                } else {
                                    IconButtonDefaults.filledIconButtonColors()
                                }
                            ) {
                                Text(text = "S", fontFamily = worldOneRegular)
                            }
                        }
                    }
                }
            }
            Card {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    FilledIconToggleButton(checked = showCameraControls, onCheckedChange = {
                        showCameraControls = it
                    }) {
                        Icon(
                            painterResource(Res.drawable.videocam),
                            contentDescription = "Toggle camera controls"
                        )
                    }
                    val primaryColor =
                        IconButtonDefaults.iconButtonColors()
                            .copy(contentColor = MaterialTheme.colorScheme.primary)
                    IconButton(
                        onClick = {
                            callAction(Midis2jam2Action.SeekBackward)
                        },
                        colors = primaryColor
                    ) {
                        Icon(painterResource(Res.drawable.replay_10), "")
                    }

                    IconButton(
                        onClick = {
                            callAction(Midis2jam2Action.PlayPause)
                        },
                        colors = primaryColor
                    ) {
                        Icon(painterResource(Res.drawable.play_pause), "")
                    }

                    IconButton(
                        onClick = {
                            callAction(Midis2jam2Action.SeekForward)
                        },
                        colors = primaryColor
                    ) {
                        Icon(painterResource(Res.drawable.forward_10), "")
                    }
                }
            }
        }
    }
}

private suspend fun PointerInputScope.detectGestures(
    onTap: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onFingerCountChanged: (Int) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var rotation = 0f
        var centroid = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var dragAmount = Offset.Zero

        // First touch - check if it's already consumed
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        var fingerCount = 1
        onFingerCountChanged(fingerCount)

        // Track if this gesture started on a consumed event
        val initialEventConsumed = firstDown.isConsumed

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.positionChanged() && it.isConsumed }

            // Update finger count
            val currentFingerCount = event.changes.size
            if (currentFingerCount != fingerCount) {
                fingerCount = currentFingerCount
                onFingerCountChanged(fingerCount)
            }

            if (!canceled) {
                // Handle multi-touch gestures
                if (event.changes.size > 1) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange
                        pan += panChange

                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                        val panMotion = pan.getDistance()

                        if (zoomMotion > touchSlop ||
                            rotationMotion > touchSlop ||
                            panMotion > touchSlop
                        ) {
                            pastTouchSlop = true
                        }
                    }

                    if (pastTouchSlop) {
                        centroid = event.calculateCentroid(useCurrent = false)
                        val effectiveZoom = if (zoomChange != 1f) zoomChange else 1f
                        onTransform(centroid, panChange, effectiveZoom, rotationChange)
                        event.changes.forEach { it.consume() }
                    }
                } else {
                    // Handle single-touch drag
                    val change = event.changes[0]
                    if (change.positionChanged()) {
                        val currentDragAmount = change.positionChange()

                        if (!pastTouchSlop) {
                            dragAmount += currentDragAmount
                            if (dragAmount.getDistance() > touchSlop) {
                                pastTouchSlop = true
                            }
                        }

                        if (pastTouchSlop) {
                            onDrag(change, currentDragAmount)
                            change.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        // Only consider it a tap if the initial event wasn't consumed and no slop was crossed
        if (!pastTouchSlop && fingerCount == 1 && !initialEventConsumed) {
            onTap()
        }
    }
}
