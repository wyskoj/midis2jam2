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

package org.wysko.midis2jam2.instrument.family.piano

/**
 * Defines how the models and textures of a key should be configured.
 * The configuration can be based on whether the key makes up/down distinction
 * either based on separate models or separate textures.
 */
sealed class KeyConfiguration {
    /**
     * Separate models for each key state (up/down).
     * @property frontKeyFile The file path of the model for the front of the key in the up state.
     * @property backKeyFile The file path of the model for the back of the key in the up state. Null if not applicable.
     * @property frontKeyFileDown The file path of the model for the front of the key in the down state.
     * @property backKeyFileDown The file path of the model for the back of the key in the down state. Null if not applicable.
     * @property texture The texture used for all states of the key.
     */
    data class SeparateModels(
        val frontKeyFile: String,
        val backKeyFile: String?,
        val frontKeyFileDown: String,
        val backKeyFileDown: String?,
        val texture: String
    ) : KeyConfiguration()

    /**
     * Same model with separate textures for each key state (up/down).
     * @property frontKeyFile The file path of the model for the front of the key.
     * @property backKeyFile The file path of the model for the back of the key. Null if not applicable.
     * @property upTexture The texture used when the key is in the up state.
     * @property downTexture The texture used when the key is in the down state.
     */
    data class SeparateTextures(
        val frontKeyFile: String,
        val backKeyFile: String?,
        val upTexture: String,
        val downTexture: String
    ) : KeyConfiguration()
}

/**
 * Contains configurations for both white and black keys in a keyboard.
 * @property whiteKeyConfiguration The configuration for white keys.
 * @property blackKeyConfiguration The configuration for black keys.
 */
data class KeyboardConfiguration(
    val whiteKeyConfiguration: KeyConfiguration,
    val blackKeyConfiguration: KeyConfiguration
)