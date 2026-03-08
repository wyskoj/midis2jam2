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

package org.wysko.midis2jam2.ui.common.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.background_cubemap_warning_continue
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.wallpaper
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.BackgroundWarning

/**
 * A warning dialog shown when the user attempts to start playback with a cube map background
 * misconfiguration:
 * - [BackgroundWarning.UNASSIGNED]: not all 6 images have been assigned.
 * - [BackgroundWarning.MISSING]: some specified image files cannot be found on disk.
 *
 * @param warningType The type of background misconfiguration.
 * @param onConfirm Called when the user chooses to continue anyway.
 * @param onDismiss Called when the user cancels.
 */
@Composable
fun BackgroundWarningDialog(
    warningType: BackgroundWarning,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    val title = backgroundWarningTitle(warningType)
    val message = backgroundWarningMessage(warningType)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        icon = {
            Icon(
                painterResource(Res.drawable.wallpaper),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = textButtonColors) {
                Text(stringResource(Res.string.background_cubemap_warning_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = textButtonColors) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
