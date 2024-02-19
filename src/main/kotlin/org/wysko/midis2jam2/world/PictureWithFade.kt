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

package org.wysko.midis2jam2.world

import com.jme3.asset.AssetManager
import com.jme3.material.RenderState.BlendMode.Alpha
import com.jme3.math.ColorRGBA
import com.jme3.scene.Node
import com.jme3.texture.Texture2D
import com.jme3.ui.Picture

/**
 * A type of [Picture] that allows for an easy-to-modify opacity setting.
 * It is also wrapped within a [Node] so it is scalable.
 *
 * @param assetManager The asset manager.
 * @param texture The texture to use.
 */
class PictureWithFade(assetManager: AssetManager, texture: String) : Node() {

    private val picture: Picture = Picture(texture).apply {
        (assetManager.loadTexture(texture) as Texture2D).also {
            setTexture(assetManager, it, true)
            setWidth(it.image.width.toFloat())
            setHeight(it.image.height.toFloat())
        }
        material = this.material.clone().apply {
            additionalRenderState.blendMode = Alpha
        }
    }.also {
        this.attachChild(it)
    }

    /**
     * The opacity of this sprite.
     */
    var opacity: Float = 1f
        set(value) {
            this.picture.material.setColor("Color", ColorRGBA(1f, 1f, 1f, value))
            field = value
        }
}
