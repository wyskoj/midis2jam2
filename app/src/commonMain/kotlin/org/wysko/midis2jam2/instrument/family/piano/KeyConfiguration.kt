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

import org.wysko.midis2jam2.assets.ModelAsset
import org.wysko.midis2jam2.assets.MaterialAsset

/**
 * Defines how the models and textures of a key should be configured.
 * The configuration can be based on whether the key makes up/down distinction
 * either based on separate models or separate textures.
 */
sealed class KeyConfiguration {
    /**
     * Separate models for each key state (up/down).
     * @property frontKeyFile The model for the front of the key in the up state.
     * @property backKeyFile The model for the back of the key in the up state. Null if not applicable.
     * @property frontKeyFileDown The model for the front of the key in the down state.
     * @property backKeyFileDown The model for the back of the key in the down state. Null if not applicable.
     * @property texture The material used for all states of the key.
     */
    data class SeparateModels(
        val frontKeyFile: ModelAsset,
        val backKeyFile: ModelAsset?,
        val frontKeyFileDown: ModelAsset,
        val backKeyFileDown: ModelAsset?,
        val texture: MaterialAsset
    ) : KeyConfiguration()

    /**
     * Same model with separate textures for each key state (up/down).
     * @property frontKeyFile The model for the front of the key.
     * @property backKeyFile The model for the back of the key. Null if not applicable.
     * @property upTexture The material used when the key is in the up state.
     * @property downTexture The material used when the key is in the down state.
     */
    data class SeparateTextures(
        val frontKeyFile: ModelAsset,
        val backKeyFile: ModelAsset?,
        val upTexture: MaterialAsset,
        val downTexture: MaterialAsset
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