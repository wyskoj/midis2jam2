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

package org.wysko.midis2jam2.ui.common.component.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsBooleanCard(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    isEnabled: Boolean,
    setIsEnabled: (Boolean) -> Unit,
) {
    SettingsCard({ setIsEnabled(!isEnabled) }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            icon()
            Column(
                modifier = Modifier.weight(1f)
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)) {
                    title()
                }
                ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                    label()
                }
            }
            Switch(isEnabled, onCheckedChange = setIsEnabled)
        }
    }
}