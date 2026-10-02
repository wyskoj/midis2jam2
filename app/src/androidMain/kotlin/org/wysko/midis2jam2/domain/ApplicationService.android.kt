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

package org.wysko.midis2jam2.domain

import android.content.Context
import android.content.Intent
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.PerformanceActivity
import org.wysko.midis2jam2.starter.configuration.PerformanceConfig
import org.wysko.midis2jam2.starter.configuration.PerformanceConfigFactory

actual class ApplicationService : KoinComponent {
    private val _isApplicationRunning = MutableStateFlow(false)
    actual val isApplicationRunning: StateFlow<Boolean>
        get() = _isApplicationRunning

    private val _midiFile = MutableStateFlow<PlatformFile?>(null)
    val midiFile: StateFlow<PlatformFile?>
        get() = _midiFile

    private val _config = MutableStateFlow<PerformanceConfig?>(null)
    val config: StateFlow<PerformanceConfig?>
        get() = _config

    private val onboardingPersistor = OnboardingPersistor()
    private val _isFirstLaunch = MutableStateFlow(onboardingPersistor.isFirstLaunch)
    val isFirstLaunch: StateFlow<Boolean>
        get() = _isFirstLaunch

    actual fun startApplication(executionState: ExecutionState) {
        _isApplicationRunning.value = true

        onboardingPersistor.markLaunched()
        _isFirstLaunch.value = false

        val factory: PerformanceConfigFactory by inject()
        val config = factory.create(isLooping = executionState.isLooping)
        val midiFile = executionState.midiFile

        this._midiFile.value = midiFile
        this._config.value = config

        val context: Context by inject()
        context.startActivity(
            Intent(context, PerformanceActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun onApplicationFinished() {
        _isApplicationRunning.value = false
    }

    actual fun startQueueApplication(executionState: QueueExecutionState) = Unit // Not implemented for Android
}
