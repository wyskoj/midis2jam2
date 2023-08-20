/*
 * Copyright (C) 2023 Jacob Wysko
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

package org.wysko.midis2jam2.starter.configuration

import kotlinx.serialization.Serializable
import java.io.File

private val CONFIG_FILE = File(APPLICATION_CONFIG_HOME, "graphics.json")

/**
 * Serializable data class representing the graphics configuration of the application.
 *
 * It contains properties related to the graphics settings of a window,
 * such as the window resolution, shadow quality, and antialiasing settings.
 *
 * @property windowResolution The resolution of the window.
 * @property shadowQuality The quality scale of shadows.
 * @property antiAliasingQuality The quality scale of antialiasing.
 */
@Serializable
data class GraphicsConfiguration(
    val windowResolution: Resolution = Resolution.DefaultResolution,
    val shadowQuality: QualityScale = QualityScale.MEDIUM,
    val antiAliasingQuality: QualityScale = QualityScale.LOW,
) : Configuration {
    companion object {
        val preserver by lazy { ConfigurationPreserver(serializer(), serializer(), CONFIG_FILE) }
        val ANTI_ALIASING_DEFINITION = mapOf(
            QualityScale.NONE to 1,
            QualityScale.LOW to 2,
            QualityScale.MEDIUM to 3,
            QualityScale.HIGH to 4,
        )
        val SHADOW_DEFINITION = mapOf(
            QualityScale.LOW to (1 to 1024),
            QualityScale.MEDIUM to (2 to 2048),
            QualityScale.HIGH to (4 to 4096),
        )
    }
}

/**
 * Represents a resolution for display and rendering.
 */
@Serializable
sealed class Resolution {
    /**
     * Represents the default resolution for the application. This is the resolution that the application will use if no
     * other resolution is specified.
     */
    @Serializable
    object DefaultResolution : Resolution() {
        override fun toString() = "Default"
    }

    /**
     * Represents a custom resolution for the application.
     */
    @Serializable
    data class CustomResolution(val width: Int, val height: Int) : Resolution() {
        override fun toString() = "${width}x$height"
    }

    companion object {
        val OPTIONS by lazy {
            listOf(
                DefaultResolution,
                CustomResolution(640, 480),
                CustomResolution(800, 600),
                CustomResolution(1024, 768),
                CustomResolution(1280, 720),
                CustomResolution(1280, 768),
                CustomResolution(1280, 800),
                CustomResolution(1280, 960),
                CustomResolution(1280, 1024),
                CustomResolution(1360, 768),
                CustomResolution(1366, 768),
                CustomResolution(1400, 1050),
                CustomResolution(1440, 900),
                CustomResolution(1600, 900),
                CustomResolution(1600, 1024),
                CustomResolution(1600, 1200),
                CustomResolution(1680, 1050),
                CustomResolution(1920, 1080),
                CustomResolution(1920, 1200),
                CustomResolution(2048, 1152),
                CustomResolution(2560, 1440),
                CustomResolution(2560, 1600),
                CustomResolution(3840, 2160),
                CustomResolution(4096, 2160),
            )
        }
    }
}

/**
 * Represents the quality scale of a certain item.
 *
 * The available quality scales are:
 * - [QualityScale.NONE]: Indicates that the item has no quality.
 * - [QualityScale.LOW]: Indicates that the item has low quality.
 * - [QualityScale.MEDIUM]: Indicates that the item has medium quality.
 * - [QualityScale.HIGH]: Indicates that the item has high quality.
 */
@Serializable
enum class QualityScale {
    NONE, LOW, MEDIUM, HIGH
}