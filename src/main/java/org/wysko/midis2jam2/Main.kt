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

package org.wysko.midis2jam2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.*
import org.wysko.midis2jam2.gui.Launcher
import org.wysko.midis2jam2.gui.UpdateChecker.checkForUpdates

/**
 * Where it all begins.
 */
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "midis2jam2 launcher",
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center)
            ),
            icon = BitmapPainter(useResource("ico/icon32.png", ::loadImageBitmap))
        ) {
            Launcher()
        }
        checkForUpdates() // I'm checking for updates, whether you like it or not.
    }


}