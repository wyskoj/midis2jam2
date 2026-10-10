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

package org.wysko.midis2jam2.starter

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.os.SystemClock
import android.view.MotionEvent
import com.jme3.system.SystemListener
import com.jme3.system.android.JmeAndroidSystem
import com.jme3.system.android.OGLESContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.Midis2jam2Action
import org.wysko.midis2jam2.manager.camera.CameraStateListener
import org.wysko.midis2jam2.util.logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * The most pixels the scene is rendered at along the screen's short side. Phone GPUs can't fill a 1440p screen
 * at 120 Hz with the post-processing the scene uses, so taller screens render smaller and the display hardware
 * scales the picture up, which costs nothing and is hard to tell apart on such dense screens.
 */
private const val MAX_RENDER_SHORT_SIDE = 1080

/** How much smaller than a [width] by [height] view the scene is rendered. */
private fun renderScale(width: Int, height: Int): Float =
    (MAX_RENDER_SHORT_SIDE.toFloat() / minOf(width, height)).coerceAtMost(1f)

class Midis2jam2Harness(
    context: Context,
    private val onFinish: () -> Unit,
) :
    SystemListener {
    private val app: Midis2jam2Application =
        Midis2jam2Application(onFinish)
    private val ctx: OGLESContext
    val view: GLSurfaceView
    private val isShuttingDown = AtomicBoolean(false)

    init {
        app.setDisplayFps(false)
        app.setDisplayStatView(false)
        app.start()
        ctx = app.context as OGLESContext
        // Sound comes from FluidSynth, so jME's own audio renderer (OpenAL) would only cost CPU (and has crashed while
        // starting). Android's context can't be made without a renderer named, but on initialize the application
        // takes the context's settings and skips the audio if none is named there.
        ctx.settings.audioRenderer = null
        view = ctx.createView(context)
        JmeAndroidSystem.setView(view)
        ctx.systemListener = this
        view.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val scale = renderScale(right - left, bottom - top)
            view.holder.setFixedSize(((right - left) * scale).roundToInt(), ((bottom - top) * scale).roundToInt())
        }
        app.registerCameraStateListener(object : CameraStateListener {
            override fun onFreeCameraEnabled() {
                _isAutoCamActive.value = false
                _isSlideCamActive.value = false
            }

            override fun onAutoCameraEnabled() {
                _isAutoCamActive.value = true
                _isSlideCamActive.value = false
            }

            override fun onRotatingCameraEnabled() {
                _isAutoCamActive.value = false
                _isSlideCamActive.value = true
            }
        })
    }

    override fun initialize() {
        app.initialize()
    }

    override fun reshape(width: Int, height: Int) {
        app.reshape(width, height)
    }

    override fun update() {
        app.update()
    }

    override fun requestClose(esc: Boolean) {
        app.stop()
        onFinish()
    }

    override fun gainFocus() {
        app.gainFocus()
        view.requestFocus()
    }

    override fun loseFocus() {
        app.loseFocus()
        view.clearFocus()
    }

    override fun handleError(errorMsg: String?, t: Throwable?) {
        logger().error("Midis2jam2Harness: handleError() called with message: $errorMsg", t)
    }

    override fun destroy() {
        app.stop()
        app.destroy()
    }

    fun stop() {
        shutdown()
    }

    fun shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) return
        cancelPendingGestureCallbacks()
        loseFocus()
        app.stop()
    }

    private fun cancelPendingGestureCallbacks() {
        val now = SystemClock.uptimeMillis()
        val cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        try {
            view.dispatchTouchEvent(cancelEvent)
            clearPendingGestureDetectorMessages()
        } finally {
            cancelEvent.recycle()
        }
    }

    private fun clearPendingGestureDetectorMessages() {
        runCatching {
            val androidInput = getFieldValue(ctx, "androidInput") ?: return
            val touchInput = getFieldValue(androidInput, "touchInput") ?: return
            val gestureDetector =
                touchInput::class.java.getMethod("getGestureDetector").invoke(touchInput) as? GestureDetector ?: return
            val handler = getFieldValue(gestureDetector, "mHandler") as? android.os.Handler ?: return
            handler.removeCallbacksAndMessages(null)
        }.onFailure {
            logger().warn("Midis2jam2Harness: failed to clear pending gesture messages")
        }
    }

    private fun getFieldValue(instance: Any, fieldName: String): Any? {
        var currentClass: Class<*>? = instance::class.java
        while (currentClass != null) {
            try {
                val field = currentClass.getDeclaredField(fieldName).apply { isAccessible = true }
                return field.get(instance)
            } catch (_: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }

    fun callAction(action: Midis2jam2Action) {
        app.callAction(action)
    }

    fun registerProgressListener(listener: ProgressListener): Unit =
        app.registerProgressListener(listener)

    fun registerPlaybackStateListener(listener: (Boolean) -> Unit): Unit =
        app.registerPlaybackStateListener(listener)

    private val _isAutoCamActive = MutableStateFlow(false)
    val isAutoCamActive: StateFlow<Boolean>
        get() = _isAutoCamActive

    private val _isSlideCamActive = MutableStateFlow(false)
    val isSlideCamActive: StateFlow<Boolean>
        get() = _isSlideCamActive
}
