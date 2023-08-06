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

package org.wysko.midis2jam2.world

import com.jme3.asset.plugins.FileLocator
import com.jme3.bounding.BoundingSphere
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Sphere
import com.jme3.texture.Image
import com.jme3.texture.Texture
import com.jme3.texture.TextureCubeMap
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
                rootNode.attachChild(
                    Geometry("Sky", Sphere(10, 10, 10f, false, true)).apply {
                        queueBucket = RenderQueue.Bucket.Sky
                        cullHint = Spatial.CullHint.Never
                        modelBound = BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO)
                        material = Material(assetManager, "Common/MatDefs/Misc/Sky.j3md").apply {
                            setVector3("NormalScale", Vector3f.UNIT_XYZ)
                            setTexture(
                                "Texture",
                                TextureCubeMap(
                                    with(assetManager.loadTexture("Assets/sky.png")) {
                                        Image(
                                            image.format,
                                            image.width,
                                            image.height,
                                            null,
                                            image.colorSpace
                                        ).apply {
                                            repeat(6) {
                                                addData(image.data[0])
                                            }
                                        }
                                    }

                                ).apply {
                                    magFilter = Texture.MagFilter.Nearest
                                    minFilter = Texture.MinFilter.NearestNoMipMaps
                                    anisotropicFilter = 0
                                    setWrap(Texture.WrapMode.EdgeClamp)
                                }
                            )
                        }
                        shadowMode = RenderQueue.ShadowMode.Off
                    }
                )
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
