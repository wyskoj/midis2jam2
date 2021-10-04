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

import java.awt.Image
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JMenuItem

/** A JMenuItem that automatically resizes its icon to be 20x20 pixels. */
class MenuItemResizedIcon : JMenuItem() {
    /** Set icon of menu item. */
    override fun setIcon(defaultIcon: Icon) {
        super.setIcon(ImageIcon((defaultIcon as ImageIcon).image.getScaledInstance(20, 20, Image.SCALE_SMOOTH)))
    }
}