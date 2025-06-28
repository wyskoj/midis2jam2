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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsCategoryCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsCategoryCardDefaults

@get:Composable
expect val categoryGroups: List<List<SettingsCategoryCardProps>>

internal expect val graphicsCategoryDescription: StringResource

@Composable
internal expect fun SettingsScreenScaffold(content: @Composable () -> Unit)

object SettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val groups = categoryGroups

        SettingsScreenScaffold {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                groups.forEachIndexed { i, categoryGroup ->
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categoryGroup.forEachIndexed { j, category ->
                                SettingsCategoryCard(
                                    title = { Text(stringResource(category.title)) },
                                    description = { Text(stringResource(category.description)) },
                                    icon = { Icon(painterResource(category.icon), contentDescription = null) },
                                    shape = SettingsCategoryCardDefaults.itemShape(j, categoryGroup.size)
                                ) {
                                    navigator.push(category.screen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SettingsCategoryCardProps(
    val title: StringResource,
    val description: StringResource,
    val icon: DrawableResource,
    val screen: Screen,
)