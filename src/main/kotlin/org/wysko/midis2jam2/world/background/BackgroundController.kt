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
package org.wysko.midis2jam2.world.background

import com.jme3.asset.plugins.FileLocator
import com.jme3.math.ColorRGBA
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.starter.configuration.BACKGROUND_IMAGES_FOLDER
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import org.wysko.midis2jam2.util.plusAssign

/**
 * Handles configuring the background.
 */
object BackgroundController {

    /**
     * Configures the background based on the given [configuration][config].
     *
     * @param context The context to the main class.
     * @param config The configuration for the background.
     * @param root The root node of the scene.
     */
    fun configureBackground(context: Midis2jam2, config: BackgroundConfiguration, root: Node) {
        with(context) {
            assetManager.registerLocator(BACKGROUND_IMAGES_FOLDER.absolutePath, FileLocator::class.java)
            when (config) {
                is BackgroundConfiguration.DefaultBackground ->
                    root += BackgroundFactory.Default(assetManager).create()

                is BackgroundConfiguration.UniqueCubeMapBackground ->
                    root += BackgroundFactory.UniqueCubeMap(assetManager, config).create()

                is BackgroundConfiguration.RepeatedCubeMapBackground ->
                    root += BackgroundFactory.RepeatedCubeMap(assetManager, config).create()

                is BackgroundConfiguration.ColorBackground ->
                    app.viewPort.backgroundColor = ColorRGBA().fromIntARGB(config.color)
            }
        }
    }
}