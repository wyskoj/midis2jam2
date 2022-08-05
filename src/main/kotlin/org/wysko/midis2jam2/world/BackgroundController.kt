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

package org.wysko.midis2jam2.world

import com.jme3.asset.plugins.FileLocator
import com.jme3.math.ColorRGBA
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import com.jme3.util.SkyFactory
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.gui.BACKGROUNDS_FOLDER
import org.wysko.midis2jam2.util.logger
import java.awt.Color

/** Handles configuring the background. */
object BackgroundController {

    /**
     * Configures the background.
     */
    fun configureBackground(
        type: String,
        value: Any?,
        context: Midis2jam2,
        rootNode: Node
    ): ViewPort? {
        val assetManager = context.assetManager

        logger().info("Configuring $type background type.")
        assetManager.registerLocator(BACKGROUNDS_FOLDER.absolutePath, FileLocator::class.java)
        when (type) {
            "DEFAULT" -> {
                with(assetManager.loadTexture("Assets/sky.png")) {
                    rootNode.attachChild(
                        SkyFactory.createSky(
                            assetManager,
                            this,
                            this,
                            this,
                            this,
                            this,
                            this
                        ).also {
                            it.shadowMode = RenderQueue.ShadowMode.Off
                        }
                    )
                }
            }

            "UNIQUE_CUBEMAP" -> {
                val names = value as List<*>
                logger().debug("Unique cubemap values: $names")
                rootNode.attachChild(
                    SkyFactory.createSky(
                        assetManager,
                        assetManager.loadTexture(names[0] as String),
                        assetManager.loadTexture(names[1] as String),
                        assetManager.loadTexture(names[2] as String),
                        assetManager.loadTexture(names[3] as String),
                        assetManager.loadTexture(names[4] as String),
                        assetManager.loadTexture(names[5] as String)
                    ).also {
                        it.shadowMode = RenderQueue.ShadowMode.Off
                    }
                )
            }

            "REPEATED_CUBEMAP" -> {
                val name = value as String
                logger().debug("Repeated cubemap texture: $name")
                assetManager.registerLocator(BACKGROUNDS_FOLDER.absolutePath, FileLocator::class.java)
                rootNode.attachChild(
                    SkyFactory.createSky(
                        assetManager,
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name)
                    ).also {
                        it.shadowMode = RenderQueue.ShadowMode.Off
                    }
                )
            }

            "COLOR" -> {
                val color = Color(value as Int)
                context.app.viewPort.backgroundColor =
                    ColorRGBA(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
            }
        }

        return null
    }
}
