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

import com.jme3.texture.Texture
import com.jme3.texture.Texture2D
import com.jme3.texture.plugins.AWTLoader
import jme3tools.converters.ImageToAwt
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp

internal actual fun rotateTexture(texture: Texture): Texture2D {
    val image = ImageToAwt.convert(texture.image, false, true, 0)
    val tx = AffineTransform.getScaleInstance(-1.0, -1.0).apply {
        translate(-image.width.toDouble(), -image.height.toDouble())
    }
    val rotatedImage = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, null)
    return Texture2D(AWTLoader().load(rotatedImage, true))
}
