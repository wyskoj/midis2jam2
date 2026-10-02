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
import org.wysko.midis2jam2.starter.configuration.PerformanceConfig
import org.wysko.midis2jam2.util.state

/**
 * Holds the [PerformanceConfig] a performance was started with, so code that only has the [Application] can read it.
 *
 * Code that is handed its configuration when it is built should take it as a parameter instead.
 */
class PerformanceConfigState(val config: PerformanceConfig) : BaseManager()

/** The configuration of the running performance. Fails if no [PerformanceConfigState] has been attached. */
val Application.performanceConfig: PerformanceConfig
    get() = state<PerformanceConfigState>()?.config
        ?: error("No PerformanceConfigState is attached; it must be attached before anything reads the configuration.")
