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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.display_settings
import midis2jam2.app.generated.resources.keyboard
import midis2jam2.app.generated.resources.media_output
import midis2jam2.app.generated.resources.screenshot_monitor
import midis2jam2.app.generated.resources.settings_background
import midis2jam2.app.generated.resources.settings_background_description
import midis2jam2.app.generated.resources.settings_controls
import midis2jam2.app.generated.resources.settings_controls_description
import midis2jam2.app.generated.resources.settings_fill
import midis2jam2.app.generated.resources.settings_general
import midis2jam2.app.generated.resources.settings_general_description
import midis2jam2.app.generated.resources.settings_graphics
import midis2jam2.app.generated.resources.settings_graphics_description
import midis2jam2.app.generated.resources.settings_on_screen_elements
import midis2jam2.app.generated.resources.settings_on_screen_elements_description
import midis2jam2.app.generated.resources.settings_playback
import midis2jam2.app.generated.resources.settings_playback_description
import midis2jam2.app.generated.resources.tab_settings
import midis2jam2.app.generated.resources.wallpaper
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsCategoryCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsCategoryCardDefaults
import org.wysko.midis2jam2.ui.settings.background.BackgroundSettingsScreen
import org.wysko.midis2jam2.ui.settings.controls.ControlsSettingsScreen
import org.wysko.midis2jam2.ui.settings.general.GeneralSettingsScreen
import org.wysko.midis2jam2.ui.settings.graphics.GraphicsSettingsScreen
import org.wysko.midis2jam2.ui.settings.onscreenelements.OnScreenElementsSettingsScreen
import org.wysko.midis2jam2.ui.settings.playback.PlaybackSettingsScreen

internal expect val graphicsCategoryDescription: StringResource

@Composable
internal expect fun SettingsScreenScaffold(content: @Composable () -> Unit)

object SettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    private val categoryGroups = listOf(
        listOf(
            SettingsCategoryCardProps(
                title = Res.string.settings_general,
                description = Res.string.settings_general_description,
                icon = Res.drawable.settings_fill,
                GeneralSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_graphics,
                description = graphicsCategoryDescription,
                icon = Res.drawable.display_settings,
                GraphicsSettingsScreen
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_background,
                description = Res.string.settings_background_description,
                icon = Res.drawable.wallpaper,
                BackgroundSettingsScreen
            )
        ),
        listOf(
            SettingsCategoryCardProps(
                title = Res.string.settings_controls,
                description = Res.string.settings_controls_description,
                icon = Res.drawable.keyboard,
                ControlsSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_playback,
                description = Res.string.settings_playback_description,
                icon = Res.drawable.media_output,
                PlaybackSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_on_screen_elements,
                description = Res.string.settings_on_screen_elements_description,
                icon = Res.drawable.screenshot_monitor,
                OnScreenElementsSettingsScreen,
            ),
        ),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        SettingsScreenScaffold {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                categoryGroups.forEachIndexed { i, categoryGroup ->
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

private data class SettingsCategoryCardProps(
    val title: StringResource,
    val description: StringResource,
    val icon: DrawableResource,
    val screen: Screen,
)