/*
 * Copyright (C) 2021 Jacob Wysko
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
package org.wysko.midis2jam2.gui

import java.awt.Cursor
import java.awt.Point
import java.awt.image.BufferedImage
import javax.swing.JFrame

abstract class Displays : JFrame() {
    fun display() {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    /**
     * Shows or hides the cursor.
     *
     * @param hide if true, hides the cursor, false shows the cursor
     */
    fun hideCursor(hide: Boolean) {
        cursor = if (hide) {
            this.toolkit.createCustomCursor(
                BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
                Point(),
                null
            )
        } else {
            Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        }
    }
}