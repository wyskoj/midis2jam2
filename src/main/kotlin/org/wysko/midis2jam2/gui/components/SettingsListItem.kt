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

package org.wysko.midis2jam2.gui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Function to display a settings list item with a headline text, supporting text, state, and options.
 *
 * @param headlineText The headline text to be displayed. Must be a composable function.
 * @param supportingText The supporting text to be displayed. Optional and can be null. Must be a composable function.
 * @param state The state of the settings item. Optional and can be null. Must be a State<Boolean>.
 * @param setState The function to set the state of the settings item.
 * Optional and can be null.
 * Must be a function that takes a Boolean parameter.
 * @param onOpenOptions The function to be called when options are opened. Optional and can be null. Must be a function with no parameters.
 */
@Composable
fun SettingsListItem(
    headlineText: @Composable () -> Unit,
    supportingText: @Composable (() -> Unit)? = null,
    state: State<Boolean>? = null,
    setState: ((Boolean) -> Unit)? = null,
    onOpenOptions: (() -> Unit)? = null,
) {
    val baseModifier = Modifier.height(72.dp)
    ListItem(
        headlineContent = headlineText,
        supportingContent = supportingText,
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenOptions != null && state != null) {
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                }
                state?.let { state ->
                    Switch(
                        checked = state.value,
                        onCheckedChange = { setState?.invoke(it) }
                    )
                }
            }
        },
        modifier = if (onOpenOptions != null) {
            baseModifier.clickable { onOpenOptions() }
        } else {
            baseModifier.clickable {
                state?.let {
                    setState?.invoke(!it.value)
                }
            }
        }
    )
}