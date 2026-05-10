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

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.ui.common.material.AppTheme
import org.wysko.midis2jam2.ui.performance.PerformanceContent
import org.wysko.midis2jam2.util.hideSystemBars
import org.wysko.midis2jam2.util.keepScreenOn
import kotlin.time.Duration.Companion.milliseconds

class PerformanceActivity : ComponentActivity(), KoinComponent {
    private val applicationService: ApplicationService by inject()
    private var isPerformanceFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        keepScreenOn()
        hideSystemBars()

        setContent {
            AppTheme {
                PerformanceContent(
                    applicationService = applicationService,
                    onFinish = ::finishPerformance
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            repeat(20) {
                delay(100.milliseconds)
                val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode
                val visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                if (!inPip && !visible) {
                    finishPerformance()
                    return@launch
                }
            }
        }
    }

    private fun finishPerformance() {
        if (isPerformanceFinished) return
        isPerformanceFinished = true
        applicationService.onApplicationFinished()
        finish()
    }
}
