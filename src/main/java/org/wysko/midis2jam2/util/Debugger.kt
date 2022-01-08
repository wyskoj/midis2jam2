/*
 * Copyright (C) 2022 Jacob Wysko
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

package org.wysko.midis2jam2.util

import org.lwjgl.opengl.GL11
import org.wysko.midis2jam2.Midis2jam2
import kotlin.system.measureTimeMillis

/**
 * Generates debug text.
 */
fun Midis2jam2.debugText(tpf: Float, time: Double): String {
    return buildString {
        measureTimeMillis {
            /* midis2jam2 version and build */
            append("midis2jam2 v${this@debugText.version} (${this@debugText.build})\n")

            /* computer operating system and renderer */
            append("$operatingSystem\n")
            append("${GL11.glGetString(GL11.GL_RENDERER)}\n")

            /* settings */
            append("F: ${this@debugText.settings.fullscreen} / LDE: ${this@debugText.settings.legacyDisplayEngine}\n")
            append("D: ${this@debugText.settings.midiDevice} / SF: ${this@debugText.settings.soundFont?.name ?: "N/A"}\n")
            append("LF: ${this@debugText.settings.latencyFix}\n")

            /* fps and time */
            append("${String.format("%.0f", 1 / tpf)} fps\n")
            append("${String.format("%.2f", time)}s\n")

            /* instruments strings */
            append("${this@debugText.instruments.joinToString("")}\n")
        }
    }
}

private val operatingSystem = "${System.getProperty("os.name")} / ${System.getProperty("os.version")}"