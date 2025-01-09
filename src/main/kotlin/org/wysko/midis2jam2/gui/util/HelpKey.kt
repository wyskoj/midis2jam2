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

package org.wysko.midis2jam2.gui.util

import java.awt.Desktop
import java.net.URI

private const val HELP_URL = "https://docs.midis2jam2.xyz"

/**
 * Opens the help page in the user's default browser.
 */
fun openHelp(): Unit = Desktop.getDesktop().browse(URI(HELP_URL))

/**
 * Opens the help page in the user's default browser.
 */
fun openHelp(vararg page: String): Unit =
    Desktop.getDesktop().browse(URI(page.joinToString("/", prefix = "$HELP_URL/")))
