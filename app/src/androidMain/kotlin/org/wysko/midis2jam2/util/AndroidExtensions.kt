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

package org.wysko.midis2jam2.util

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.starter.Midis2jam2Harness
import kotlin.math.PI
import kotlin.math.abs

fun View.systemBackLegacy(
    harness: Midis2jam2Harness,
    applicationService: ApplicationService,
    onFinish: () -> Unit,
) {
    isFocusableInTouchMode = true
    requestFocus()
    setOnKeyListener { _, keyCode, _ ->
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            harness.stop()
            applicationService.onApplicationFinished()
            onFinish()
            true
        } else {
            false
        }
    }
}

fun ComponentActivity.hideSystemBars() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun rememberIsInPipMode(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val activity = LocalContext.current.findActivity()
        var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
        DisposableEffect(activity) {
            val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
            activity.addOnPictureInPictureModeChangedListener(
                observer
            )
            onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
        }
        return pipMode
    } else {
        return false
    }
}

fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error("Picture in picture should be called in the context of an Activity")
}

@Composable
fun EnablePipForPreAndroid12(context: Context, shouldEnterPipMode: Boolean) {
    val currentShouldEnterPipMode by rememberUpdatedState(newValue = shouldEnterPipMode)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        DisposableEffect(context) {
            val onUserLeaveBehavior = Runnable {
                if (currentShouldEnterPipMode) {
                    context.findActivity().enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                }
            }
            context.findActivity().addOnUserLeaveHintListener(onUserLeaveBehavior)
            onDispose {
                context.findActivity().removeOnUserLeaveHintListener(onUserLeaveBehavior)
            }
        }
    } else {
        Log.i("PiP info", "API does not support PiP")
    }
}

fun Activity.keepScreenOn() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

suspend fun PointerInputScope.detectGestures(
    onTap: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onFingerCountChanged: (Int) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Companion.Zero
        var rotation = 0f
        var centroid = Offset.Companion.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var dragAmount = Offset.Companion.Zero

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