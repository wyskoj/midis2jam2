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

import org.wysko.midis2jam2.starter.ProgressListener
import org.wysko.midis2jam2.util.state

class LoadingProgressManager : BaseManager() {
    private val listeners = mutableListOf<ProgressListener>()
    private var hasDispatchedReady = false

    internal fun registerProgressListener(listener: ProgressListener) {
        listeners.add(listener)
    }

    internal fun onLoadingAsset(assetName: String) {
        listeners.forEach { it.onLoadingAsset(assetName) }
    }

    internal fun onLoadingProgress(progress: Float) {
        listeners.forEach { it.onLoadingProgress(progress) }
    }

    internal fun onReady() {
        listeners.forEach { it.onReady() }
    }

    override fun update(tpf: Float) {
        if (!hasDispatchedReady && app.state<PerformanceManager>()?.isInitialized ?: false) {
            onReady()
            hasDispatchedReady = true
        }
    }
}