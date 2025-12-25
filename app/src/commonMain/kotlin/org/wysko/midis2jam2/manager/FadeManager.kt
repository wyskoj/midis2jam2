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

import com.jme3.app.Application
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.FadeFilter
import org.wysko.midis2jam2.manager.PlaybackManager.Companion.sequence
import org.wysko.midis2jam2.manager.PlaybackManager.Companion.time
import org.wysko.midis2jam2.util.Utils
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class FadeManager : BaseManager() {
    private lateinit var fadeFilter: FadeFilter

    val fadeValue: Float
        get() = fadeFilter.value

    override fun initialize(app: Application) {
        super.initialize(app)
        fadeFilter = FadeFilter()
    }

    override fun onEnable() {
        super.onEnable()
        getFpp()?.addFilter(fadeFilter)
    }

    override fun onDisable() {
        super.onDisable()
        getFpp()?.removeFilter(fadeFilter)
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        fadeFilter.value = when {
            app.time in (-2.0).seconds..(-1.5).seconds -> {
                0.0f
            }

            app.time in (-1.5).seconds..(-1.0).seconds -> {
                Utils.mapRangeClamped(app.time.toDouble(DurationUnit.SECONDS), -1.5, -1.0, 0.0, 1.0)
                    .toFloat()
            }

            (app.sequence.duration + 3.seconds - app.time) < 0.5.seconds -> {
                Utils.mapRangeClamped(
                    app.time.toDouble(DurationUnit.SECONDS),
                    ((app.sequence.duration + 3.seconds) - 0.5.seconds).toDouble(DurationUnit.SECONDS),
                    (app.sequence.duration + 3.seconds).toDouble(DurationUnit.SECONDS),
                    1.0,
                    0.0
                ).toFloat()
            }

            else -> 1.0f
        }
    }

    private fun getFpp() = application.viewPort.processors.filterIsInstance<FilterPostProcessor>().firstOrNull()
}