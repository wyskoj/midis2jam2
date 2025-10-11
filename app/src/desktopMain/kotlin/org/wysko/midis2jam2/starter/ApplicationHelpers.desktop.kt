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

package org.wysko.midis2jam2.starter

import com.jme3.system.AppSettings
import org.wysko.midis2jam2.starter.configuration.Resolution
import org.wysko.midis2jam2.util.isMacOs
import java.awt.GraphicsEnvironment
import javax.imageio.ImageIO

internal actual fun AppSettings.applyIcons() {
    if (isMacOs()) return // Do not set icons on macOS

    icons = arrayOf("/ico/icon16.png", "/ico/icon32.png", "/ico/icon128.png", "/ico/icon256.png")
        .map { ImageIO.read(this::class.java.getResource(it)) }
        .toTypedArray()
}

internal actual fun AppSettings.applyScreenFrequency() {
    GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayModes.firstOrNull()?.let {
        frequency = it.refreshRate
    }
}

internal actual fun getScreenResolution(): Resolution.CustomResolution? =
    with(GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode) {
        Resolution.CustomResolution(width, height)
    }
