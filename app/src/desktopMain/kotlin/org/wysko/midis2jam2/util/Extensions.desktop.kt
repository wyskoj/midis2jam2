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

package org.wysko.midis2jam2.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import org.wysko.midis2jam2.domain.SystemInteractionService
import java.awt.datatransfer.DataFlavor
import java.io.File

fun openHelpOnF1(systemInteractionService: SystemInteractionService): (KeyEvent) -> Boolean = {
    when (it.key) {
        Key.F1 -> {
            systemInteractionService.openOnlineDocumentation()
            true
        }

        else -> false
    }
}

class FileDragAndDrop(val onFileDropped: (File) -> Unit) : DragAndDropTarget {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onDrop(event: DragAndDropEvent): Boolean {
        event.awtTransferable.let {
            if (it.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val file =
                    (it.getTransferData(DataFlavor.javaFileListFlavor) as List<*>).firstOrNull()
                if (file is File) {
                    onFileDropped(file)
                }
            }
        }
        return false
    }
}

class FilesDragAndDrop(val onFilesDropped: (List<File>) -> Unit) : DragAndDropTarget {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onDrop(event: DragAndDropEvent): Boolean {
        event.awtTransferable.let {
            if (it.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val files = (it.getTransferData(DataFlavor.javaFileListFlavor) as List<*>).filterIsInstance<File>()
                onFilesDropped(files)
            }
        }
        return false
    }
}