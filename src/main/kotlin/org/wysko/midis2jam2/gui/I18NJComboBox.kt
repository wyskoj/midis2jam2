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

package org.wysko.midis2jam2.gui

import java.awt.Component
import java.util.*
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JList

/**
 * A combobox that supports changing the display of the cells to their internationalized versions.
 */
class I18NJComboBox<InternationalizableEnum>(
    /** The resource bundle from which the i18n strings can be pulled. */
    val resourceBundle: ResourceBundle
) : JComboBox<InternationalizableEnum>() {
    override fun getRenderer(): DefaultListCellRenderer = object : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component =
            super.getListCellRendererComponent(
                list,
                resourceBundle.getString((value as org.wysko.midis2jam2.gui.InternationalizableEnum).i18nKey),
                index,
                isSelected,
                cellHasFocus
            )
    }
}