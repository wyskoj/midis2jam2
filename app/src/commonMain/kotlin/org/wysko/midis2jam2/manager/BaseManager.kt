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
import com.jme3.app.SimpleApplication
import com.jme3.app.state.BaseAppState
import org.wysko.midis2jam2.manager.PerformanceManager

abstract class BaseManager() : BaseAppState() {
    protected lateinit var context: PerformanceManager

    override fun initialize(app: Application) {
        context = application.stateManager.getState(PerformanceManager::class.java)
    }

    override fun cleanup(app: Application?): Unit = Unit
    override fun onEnable(): Unit = Unit
    override fun onDisable(): Unit = Unit

    protected val app: SimpleApplication
        get() = application as SimpleApplication
}