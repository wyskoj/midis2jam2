/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.assets

/*
 * References to bundled assets. Instances come from the generated catalog (Models, Materials, Textures, built by
 * :asset-tools from sharedAssets; see docs/ASSETS.md), so a reference always names an asset that exists.
 */

/** A converted model (`.j3o`), its materials already applied. Load it with `PerformanceManager.model`. */
@JvmInline
value class ModelAsset(val path: String)

/** A material in the library: a `.j3m` file in `Assets/Materials`. */
@JvmInline
value class MaterialAsset(val path: String)

/** A texture under `Assets/Textures`. */
@JvmInline
value class TextureAsset(val path: String)
