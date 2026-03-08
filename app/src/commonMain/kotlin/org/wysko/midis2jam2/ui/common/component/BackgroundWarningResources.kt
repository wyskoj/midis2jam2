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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.background_cubemap_missing_warning_message
import midis2jam2.app.generated.resources.background_cubemap_missing_warning_title
import midis2jam2.app.generated.resources.background_cubemap_warning_message
import midis2jam2.app.generated.resources.background_cubemap_warning_title
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.BackgroundWarning

/** Amber/yellow tint used for background warning indicators. */
val WarningAmber = Color(0xFFFFC107.toInt())

/** Returns the dialog title for the given [warning] type. */
@Composable
fun backgroundWarningTitle(warning: BackgroundWarning): String = when (warning) {
    BackgroundWarning.UNASSIGNED -> stringResource(Res.string.background_cubemap_warning_title)
    BackgroundWarning.MISSING -> stringResource(Res.string.background_cubemap_missing_warning_title)
}

/** Returns the dialog message for the given [warning] type. */
@Composable
fun backgroundWarningMessage(warning: BackgroundWarning): String = when (warning) {
    BackgroundWarning.UNASSIGNED -> stringResource(Res.string.background_cubemap_warning_message)
    BackgroundWarning.MISSING -> stringResource(Res.string.background_cubemap_missing_warning_message)
}
