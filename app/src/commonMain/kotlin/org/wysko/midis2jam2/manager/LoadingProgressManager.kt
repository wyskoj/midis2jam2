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

package org.wysko.midis2jam2.manager

import org.wysko.midis2jam2.starter.LoadingStage
import org.wysko.midis2jam2.starter.ProgressListener
import org.wysko.midis2jam2.util.state
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Passes loading progress on to its listeners, and tells them when the performance is ready.
 *
 * Progress arrives from the loading coroutine and from the render thread, and listeners register from the UI thread,
 * so it can be used before it is attached. A listener that registers late is caught up on where loading has got to.
 */
class LoadingProgressManager : BaseManager() {
    private val listeners = CopyOnWriteArrayList<ProgressListener>()

    @Volatile
    private var stage: LoadingStage? = null

    @Volatile
    private var progress: Float? = null

    @Volatile
    private var isReady = false

    internal fun registerProgressListener(listener: ProgressListener) {
        listeners.add(listener)
        stage?.let(listener::onLoadingStage)
        progress?.let(listener::onLoadingProgress)
        if (isReady) listener.onReady()
    }

    internal fun onLoadingStage(stage: LoadingStage) {
        this.stage = stage
        listeners.forEach { it.onLoadingStage(stage) }
    }

    internal fun onLoadingProgress(progress: Float) {
        this.progress = progress
        listeners.forEach { it.onLoadingProgress(progress) }
    }

    private fun onReady() {
        isReady = true
        listeners.forEach { it.onReady() }
    }

    override fun update(tpf: Float) {
        if (!isReady && app.state<PerformanceManager>()?.isInitialized ?: false) onReady()
    }
}
