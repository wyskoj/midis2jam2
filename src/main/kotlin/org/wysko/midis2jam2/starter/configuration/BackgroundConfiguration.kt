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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration.*
import org.wysko.midis2jam2.util.logger
import java.io.File

/**
 * The folder where the user stores background images.
 */
val BACKGROUND_IMAGES_FOLDER: File = File(APPLICATION_CONFIG_HOME, "backgrounds").also {
    it.mkdirs()
}
private val CONFIG_FILE = File(APPLICATION_CONFIG_HOME, "background.json")

/**
 * Represents a configuration for the background of an application.
 *
 * This class is sealed and provides four subclasses to represent different types of backgrounds:
 *   - [DefaultBackground]: Represents the default background configuration.
 *   - [RepeatedCubemapBackground]: Represents a background configuration with a repeated cubemap texture.
 *   - [UniqueCubemapBackground]: Represents a background configuration with a unique cubemap texture.
 *   - [ColorBackground]: Represents a background configuration with a solid color.
 *
 * This class implements the [Configuration] interface and is serializable.
 *
 * @see DefaultBackground
 * @see RepeatedCubemapBackground
 * @see UniqueCubemapBackground
 * @see ColorBackground
 *
 * @constructor Creates an instance of [BackgroundConfiguration].
 */
@Serializable
sealed class BackgroundConfiguration : Configuration {

    /** The default background configuration. */
    @Serializable
    @SerialName("default")
    data object DefaultBackground : BackgroundConfiguration()

    /** A background configuration with a cubemap texture. */
    sealed class CubemapBackground : BackgroundConfiguration() {
        /** Ensures that the cubemap background configuration is valid. */
        abstract fun validate(): Boolean
    }

    /**
     * A background configuration with a repeated cubemap texture. It is "repeated" in the sense that one texture
     * is repeated on all six walls.
     *
     * @param texture The name of the texture to use. This name corresponds to the file name of the texture in the
     *               [BACKGROUND_IMAGES_FOLDER] directory.
     */
    @Serializable
    @SerialName("repeated")
    data class RepeatedCubemapBackground(val texture: String) : CubemapBackground() {
        override fun validate(): Boolean = texture.isNotBlank()
    }

    /**
     * A background configuration with a unique cubemap texture. It is "unique" in the sense that each wall has a
     * different texture.
     *
     * @param cubemap The cubemap texture to use.
     */
    @Serializable
    @SerialName("unique")
    data class UniqueCubemapBackground(val cubemap: CubemapTexture) : CubemapBackground() {
        override fun validate(): Boolean {
            val directions = listOf(
                cubemap.north,
                cubemap.south,
                cubemap.east,
                cubemap.west,
                cubemap.up,
                cubemap.down
            )

            return directions.all { it?.isNotBlank() == true }
        }
    }

    /**
     * A background configuration with a solid color.
     *
     * @param color The color to use.
     */
    @Serializable
    @SerialName("color")
    data class ColorBackground(val color: Int) : BackgroundConfiguration()

    companion object {
        /**
         * The [ConfigurationPreserver] instance for this class.
         */
        val preserver: ConfigurationPreserver<BackgroundConfiguration> by lazy {
            ConfigurationPreserver(
                serializer(),
                serializer(),
                CONFIG_FILE
            )
        }

        /**
         * Retrieves the list of available images from the BACKGROUND_IMAGES_FOLDER.
         *
         * @return The list of available image names. If the BACKGROUND_IMAGES_FOLDER exists and contains files, it returns
         * a list of names of those files. Otherwise, it returns an empty list.
         */
        fun getAvailableImages(): List<String> = try {
            BACKGROUND_IMAGES_FOLDER.listFiles()?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            logger().error("Unable to get available images", e)
            emptyList()
        }
    }
}

/**
 * A data class representing a cubemap texture.
 *
 * @property north The texture file path for the north face.
 * @property south The texture file path for the south face.
 * @property east The texture file path for the east face.
 * @property west The texture file path for the west face.
 * @property up The texture file path for the up face.
 * @property down The texture file path for the down face.
 */
@Serializable
data class CubemapTexture(
    var north: String? = "",
    var south: String? = "",
    var east: String? = "",
    var west: String? = "",
    var up: String? = "",
    var down: String? = ""
)