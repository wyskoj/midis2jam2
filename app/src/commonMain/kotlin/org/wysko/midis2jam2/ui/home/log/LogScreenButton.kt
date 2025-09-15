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

package org.wysko.midis2jam2.ui.home.log

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.chat
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.ErrorLogService

@Composable
fun LogScreenButton(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val model = koinInject<ErrorLogService>()
    val unreadCount = model.unreadCount.collectAsState(initial = 0)

    BadgedBox(
        badge = {
            if (unreadCount.value > 0) {
                Badge {
                    Text(unreadCount.value.toString())
                }
            }
        },
        modifier = modifier,
    ) {
        IconButton(
            onClick = {
                navigator.push(LogScreen)
            },
            colors = when (unreadCount.value) {
                0 -> IconButtonDefaults.iconButtonColors()
                else -> IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                )
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(Res.drawable.chat),
                null,
                tint = when (unreadCount.value) {
                    0 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onErrorContainer
                },
            )
        }
    }
}
