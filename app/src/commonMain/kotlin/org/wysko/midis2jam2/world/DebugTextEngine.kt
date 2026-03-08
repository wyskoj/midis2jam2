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

@file:OptIn(ExperimentalCoroutinesApi::class)

package org.wysko.midis2jam2.world

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.build_timestamp
import midis2jam2.app.generated.resources.build_version
import midis2jam2.app.generated.resources.build_version_code
import org.jetbrains.compose.resources.getString
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.wrap

internal expect fun getGlRendererInfo(): String

internal expect fun getOperatingSystemInfo(): String

class DebugTextEngine(private val context: PerformanceManager) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val buildInfo = scope.async {
        listOf(
            Res.string.build_version,
            Res.string.build_version_code,
            Res.string.build_timestamp
        ).map { getString(it) }.joinToString(" | ")
    }

    private val operatingSystemInfo = scope.async {
        getOperatingSystemInfo()
    }
    private val graphicsProcessingInfo = getGlRendererInfo()
    private val javaVirtualMachineInfo = scope.async {
        listOf("name", "vendor", "version").joinToString(" | ") { System.getProperty("java.vm.$it") }
    }

    fun getText(): String {
        val sections = listOf(
            "Build" to listOf(buildInfo.get()),
            "System" to listOf(
                operatingSystemInfo.get(),
                graphicsProcessingInfo,
                javaVirtualMachineInfo.get()
            ),
            "Settings" to listOf(context.configs.joinToString().wrap(80)),
        )
        return sections.joinToString("\n\n") { "${it.first}:\n${it.second.joinToString("\n")}" }
    }

    private fun Deferred<String>.get() = if (this.isCompleted) this.getCompleted() else "Loading..."
}
