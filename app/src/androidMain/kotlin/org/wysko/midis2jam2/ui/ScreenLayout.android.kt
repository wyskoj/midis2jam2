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

package org.wysko.midis2jam2.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun MainLayout() {
    CurrentScreen()
}

@Composable
fun AppNavigationBar() {
    val navigator = LocalTabNavigator.current
    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        tabs.forEach {
            val isTabSelected = navigator.current == it.key
            val name = stringResource(it.value.name)
            NavigationBarItem(
                selected = isTabSelected,
                onClick = { navigator.current = it.key },
                icon = { Icon(painterResource(it.value.getTab(isTabSelected)), name) },
                label = { Text(name) },
                modifier = Modifier.Companion.padding(horizontal = 8.dp)
            )
        }
    }
}
