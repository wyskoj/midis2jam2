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

package org.wysko.midis2jam2.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.settings_graphics_description
import org.jetbrains.compose.resources.StringResource
import org.wysko.midis2jam2.ui.AppNavigationRail

internal actual val graphicsCategoryDescription: StringResource
    get() = Res.string.settings_graphics_description

@Composable
internal actual fun SettingsScreenScaffold(content: @Composable (() -> Unit)) {
    Row {
        AppNavigationRail()
        content()
    }
}