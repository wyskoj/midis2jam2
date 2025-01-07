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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.ApplicationScreen
import org.wysko.midis2jam2.gui.TabFactory
import org.wysko.midis2jam2.gui.viewmodel.I18n

@Composable
fun NavigationRail(
    currentScreen: ApplicationScreen,
    tabClickHandler: (ApplicationScreen) -> Unit,
) {
    Row {
        androidx.compose.material3.NavigationRail {
            Spacer(Modifier.weight(1f))
            TabFactory.tabs.forEach {
                NavigationRailItem(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    icon = {
                        Icon(
                            if (currentScreen.uid == it.uid) it.filledIcon else it.outlinedIcon,
                            contentDescription = I18n[it.i18nKey].value,
                        )
                    },
                    label = { Text(I18n[it.i18nKey].value) },
                    selected =
                        when (currentScreen) {
                            is ApplicationScreen.ScreenWithTab -> currentScreen.uid == it.uid
                            is ApplicationScreen.ScreenWithoutTab -> currentScreen.parentScreen.uid == it.uid
                        },
                    onClick = { tabClickHandler(it) },
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
        }
        VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
    }
}
