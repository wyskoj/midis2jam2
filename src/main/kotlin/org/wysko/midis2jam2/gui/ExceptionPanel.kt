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

import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * Formats an exception for display.
 */
class ExceptionPanel(message: String, exception: Exception) : JPanel() {
    init {
        layout = BorderLayout()
        add(JLabel(message), BorderLayout.NORTH)
        add(JTextArea(exception.stackTraceToString()).apply {
            isEditable = false
        }, BorderLayout.CENTER)
    }
}