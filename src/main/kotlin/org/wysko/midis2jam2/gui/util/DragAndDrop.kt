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

package org.wysko.midis2jam2.gui.util

import androidx.compose.ui.window.FrameWindowScope
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

/**
 * Registers a drag and drop listener on the window's content pane.
 *
 * @param onFileDrop The function to call when a file is dropped.
 */
fun FrameWindowScope.registerDragAndDrop(onFileDrop: (File) -> Unit) {
    this.window.contentPane.dropTarget =
        object : DropTarget() {
            @Synchronized
            override fun drop(event: DropTargetDropEvent) {
                event.acceptDrop(DnDConstants.ACTION_REFERENCE)
                val transferData = event.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                val file = transferData?.firstOrNull() as? File ?: return
                onFileDrop(file)
            }
        }
}
