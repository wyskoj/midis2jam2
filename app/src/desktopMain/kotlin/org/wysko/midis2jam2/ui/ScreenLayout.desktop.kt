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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun MainLayout() {
    Row {
        AppNavigationRail()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CurrentScreen()
        }
    }
}

@Composable
fun AppNavigationRail() {
    val navigator = LocalTabNavigator.current
    Row {
        NavigationRail {
            Spacer(Modifier.Companion.weight(1f))
            tabs.forEach {
                val isTabSelected = navigator.current == it.key
                val name = stringResource(it.value.name)
                NavigationRailItem(
                    selected = isTabSelected,
                    onClick = { navigator.current = it.key },
                    icon = { Icon(painterResource(it.value.getTab(isTabSelected)), name) },
                    label = { Text(name) },
                    modifier = Modifier.Companion.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.Companion.height(12.dp))
            }
            Spacer(Modifier.Companion.weight(1f))
        }
        VerticalDivider(Modifier.fillMaxHeight())
    }
}
