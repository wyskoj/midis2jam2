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
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.ui.Picture
import com.jme3.util.SkyFactory
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.gui.backgroundsFolder
import org.wysko.midis2jam2.util.logger

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
        val renderManager = context.renderManager

        logger().info("Configuring $type background type.")
        assetManager.registerLocator(backgroundsFolder.absolutePath, FileLocator::class.java)
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
                        )
                    )
                }
            }

            "FIXED" -> {
                val picture = Picture("background").apply {
                    setImage(assetManager, value as String, false)
                    setWidth(context.app.camera.width.toFloat())
                    setHeight(context.app.camera.height.toFloat())
                    setPosition(0f, 0f)
                    updateGeometricState()
                }
                val viewport = renderManager.createPreView("background", context.app.camera)
                viewport.setClearFlags(true, true, true)
                viewport.attachScene(Node().also { node ->
                    with(picture) {
                        node.attachChild(this)
                        cullHint = Spatial.CullHint.Never
                    }
                    node.updateGeometricState()
                    node.cullHint = Spatial.CullHint.Never
                })

                context.app.viewPort.setClearFlags(false, true, false)

                return viewport
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
                        assetManager.loadTexture(names[5] as String),
                    )
                )
            }

            "REPEATED_CUBEMAP" -> {
                val name = value as String
                logger().debug("Repeated cubemap texture: $name")
                assetManager.registerLocator(backgroundsFolder.absolutePath, FileLocator::class.java)
                rootNode.attachChild(
                    SkyFactory.createSky(
                        assetManager,
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                        assetManager.loadTexture(name),
                    )
                )
            }
        }

        return null
    }
}