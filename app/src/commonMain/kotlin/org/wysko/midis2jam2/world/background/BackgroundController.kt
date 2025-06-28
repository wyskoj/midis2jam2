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
package org.wysko.midis2jam2.world.background

import Platform
import com.jme3.asset.plugins.FileLocator
import com.jme3.math.ColorRGBA
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.starter.configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.BACKGROUND_IMAGES_FOLDER

object BackgroundController {
    fun configureBackground(context: Midis2jam2, config: AppSettingsConfiguration, root: Node, platform: Platform) {
        with(context) {
            if (platform == Platform.Desktop) {
                assetManager.registerLocator(BACKGROUND_IMAGES_FOLDER.absolutePath, FileLocator::class.java)
            }
            when (config.appSettings.backgroundSettings.type) {
                AppSettings.BackgroundSettings.BackgroundType.Default -> {
                    root.attachChild(BackgroundFactory.Default(assetManager).create())
                }

                AppSettings.BackgroundSettings.BackgroundType.RepeatedCubeMap -> {
                    root.attachChild(BackgroundFactory.RepeatedCubeMap(assetManager, config).create())
                }

                AppSettings.BackgroundSettings.BackgroundType.UniqueCubeMap -> {
                    root.attachChild(BackgroundFactory.UniqueCubeMap(assetManager, config).create())
                }

                AppSettings.BackgroundSettings.BackgroundType.Color -> {
                    app.viewPort.backgroundColor = ColorRGBA().fromIntARGB(config.appSettings.backgroundSettings.color)
                }
            }
        }
    }
}
