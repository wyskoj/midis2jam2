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

package org.wysko.midis2jam2.gui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import midis2jam2.generated.resources.Res
import midis2jam2.generated.resources.midis2jam2_logo
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.gui.viewmodel.I18n

/**
 * Displays the midis2jam2 logo.
 */
@Composable
fun Midis2jam2Logo(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(Res.drawable.midis2jam2_logo),
        contentDescription = I18n["midis2jam2"].value,
        modifier = modifier,
    )
}