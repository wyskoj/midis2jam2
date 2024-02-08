/*
 * Copyright (C) 2024 Jacob Wysko
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
import org.wysko.midis2jam2.Midis2jam2
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
        /**
         * The preserver for the graphics configuration.
         */
        val preserver: ConfigurationPreserver<GraphicsConfiguration> by lazy {
            ConfigurationPreserver(serializer(), serializer(), CONFIG_FILE)
        }

        /**
         * The definition of the antialiasing quality scale.
         *
         * This variable is a map that defines the correlation between each `QualityScale` and an integer value.
         * The integer value represents the level of antialiasing quality for that particular `QualityScale`.
         *
         * @see QualityScale
         */
        val ANTI_ALIASING_DEFINITION: Map<QualityScale, Int> = mapOf(
            QualityScale.NONE to 1,
            QualityScale.LOW to 2,
            QualityScale.MEDIUM to 3,
            QualityScale.HIGH to 4,
        )

        /**
         * The definition of the shadow quality scale.
         *
         * This variable is a map that defines the correlation between each `QualityScale` and a pair of integers.
         * The pair of integers represents the level of shadow quality for that particular `QualityScale`.
         * The first integer represents the level of shadow quality,
         * and the second integer represents the resolution of the shadow.
         *
         * @see QualityScale
         */
        val SHADOW_DEFINITION: Map<QualityScale, Pair<Int, Int>> = mapOf(
            QualityScale.LOW to (1 to 1024),
            QualityScale.MEDIUM to (2 to 2048),
            QualityScale.HIGH to (4 to 4096),
        )

        /**
         * Convenience method to determine if fake shadows should be used.
         */
        val Midis2jam2.isFakeShadows: Boolean
            get() = configs.getType(GraphicsConfiguration::class).shadowQuality == QualityScale.NONE
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
        override fun toString(): String = "Default"
    }

    /**
     * Represents a custom resolution for the application.
     *
     * @property width The width of the resolution.
     * @property height The height of the resolution.
     */
    @Serializable
    data class CustomResolution(val width: Int, val height: Int) : Resolution() {
        override fun toString(): String = "${width}x$height"
    }

    companion object {
        /**
         * The available resolution options.
         */
        val OPTIONS: List<Resolution> by lazy {
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
 */
@Serializable
enum class QualityScale {
    /**
     * Represents the lowest quality.
     */
    NONE,

    /**
     * Represents low quality.
     */
    LOW,

    /**
     * Represents medium quality.
     */
    MEDIUM,

    /**
     * Represents high quality.
     */
    HIGH
}
