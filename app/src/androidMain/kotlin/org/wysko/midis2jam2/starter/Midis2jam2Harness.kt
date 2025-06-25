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
import com.jme3.system.SystemListener
import com.jme3.system.android.JmeAndroidSystem
import com.jme3.system.android.OGLESContext
import org.wysko.midis2jam2.Midis2jam2Action
import org.wysko.midis2jam2.util.logger

class Midis2jam2Harness(
    context: Context,
    private val onFinish: () -> Unit,
) :
    SystemListener {
    private val app: Midis2jam2Application =
        Midis2jam2Application(onFinish)
    private val ctx: OGLESContext
    val view: GLSurfaceView

    init {
        app.start()
        ctx = app.context as OGLESContext
        view = ctx.createView(context)
        JmeAndroidSystem.setView(view)
        ctx.systemListener = this
    }

    override fun initialize() {
        app.initialize()
    }

    override fun reshape(width: Int, height: Int) {
        app.reshape(width, height)
        view.layout(0, 0, width, height)
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
        app.enqueue { app.stop() }
    }

    fun callAction(action: Midis2jam2Action) {
        app.callAction(action)
    }

    fun registerProgressListener(listener: ProgressListener): Unit = app.registerProgressListener(listener)
}
