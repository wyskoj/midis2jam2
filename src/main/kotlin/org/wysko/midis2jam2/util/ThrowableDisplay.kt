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

package org.wysko.midis2jam2.util

import org.wysko.midis2jam2.gui.ExceptionPanel
import org.wysko.midis2jam2.starter.Execution
import javax.swing.JOptionPane

/** Defines a utility method for handling throwables. */
object ThrowableDisplay {
    /**
     * Displays the throwable to the screen.
     */
    fun Throwable.display(
        title: String = "Error",
        message: String = "An error occurred.",
        log: Boolean = true
    ) {
        JOptionPane.showMessageDialog(null, ExceptionPanel(message, this), title, JOptionPane.ERROR_MESSAGE)
        if (log) Execution.logger().error(message, this)
    }
}
