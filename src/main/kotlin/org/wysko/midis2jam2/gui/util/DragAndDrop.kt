/*
 * Copyright (C) 2023 Jacob Wysko
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
 * Registers drag and drop functionality to the [FrameWindowScope].
 *
 * @param setMidiFile The callback function to be called when a [file][File] is dropped.
 */
fun FrameWindowScope.registerDragAndDrop(
    setMidiFile: (File) -> Unit,
) {
    this.window.contentPane.dropTarget = object : DropTarget() {
        @Synchronized
        override fun drop(dtde: DropTargetDropEvent) {
            dtde.let { dropTargetDropEvent ->
                dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_REFERENCE)
                (dropTargetDropEvent.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>).firstOrNull()
                    ?.let { setMidiFile(it as File) }
            }
        }
    }
}