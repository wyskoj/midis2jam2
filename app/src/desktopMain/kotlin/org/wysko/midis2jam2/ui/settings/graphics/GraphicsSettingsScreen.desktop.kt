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

package org.wysko.midis2jam2.ui.settings.graphics

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.close
import midis2jam2.app.generated.resources.fullscreen
import midis2jam2.app.generated.resources.high_density
import midis2jam2.app.generated.resources.hotel_class
import midis2jam2.app.generated.resources.monitor
import midis2jam2.app.generated.resources.quality_high
import midis2jam2.app.generated.resources.quality_low
import midis2jam2.app.generated.resources.quality_medium
import midis2jam2.app.generated.resources.quality_none
import midis2jam2.app.generated.resources.radio_button_unchecked
import midis2jam2.app.generated.resources.settings_graphics
import midis2jam2.app.generated.resources.settings_graphics_anti_aliasing
import midis2jam2.app.generated.resources.settings_graphics_fullscreen
import midis2jam2.app.generated.resources.settings_graphics_fullscreen_description
import midis2jam2.app.generated.resources.settings_graphics_resolution
import midis2jam2.app.generated.resources.settings_graphics_shadows
import midis2jam2.app.generated.resources.settings_graphics_shadows_none
import midis2jam2.app.generated.resources.star
import midis2jam2.app.generated.resources.tonality
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsGenericCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOptionsCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.ui.settings.graphics.resolution.ResolutionSettingsScreen

@Composable
internal actual fun GraphicsSettingsScreenContent() {
    val navigator = LocalNavigator.currentOrThrow
    val settingsModel = GraphicsSettingsScreen.koinScreenModel<SettingsModel>()
    val model = GraphicsSettingsScreen.koinScreenModel<GraphicsSettingsScreenModel>()
    val settings = settingsModel.appSettings.collectAsState()

    val shadowsQualityOptions = listOf(
        SettingsOption(
            value = ShadowsQuality.Fake,
            title = stringResource(Res.string.settings_graphics_shadows_none),
            icon = { Icon(painterResource(Res.drawable.close), "") },
        ),
        SettingsOption(
            value = ShadowsQuality.Low,
            title = stringResource(Res.string.quality_low),
            icon = { Icon(painterResource(Res.drawable.radio_button_unchecked), "") },
        ),
        SettingsOption(
            value = ShadowsQuality.Medium,
            title = stringResource(Res.string.quality_medium),
            icon = { Icon(painterResource(Res.drawable.star), "") },
        ),
        SettingsOption(
            value = ShadowsQuality.High,
            title = stringResource(Res.string.quality_high),
            icon = { Icon(painterResource(Res.drawable.hotel_class), "") },
        )
    )

    val antiAliasingQualityOptions = listOf<SettingsOption<AntiAliasingQuality?>>(
        SettingsOption(
            value = null,
            title = stringResource(Res.string.quality_none),
            icon = { Icon(painterResource(Res.drawable.close), "") },
        ),
        SettingsOption(
            value = AntiAliasingQuality.Low,
            title = stringResource(Res.string.quality_low),
            icon = { Icon(painterResource(Res.drawable.radio_button_unchecked), "") },
        ),
        SettingsOption(
            value = AntiAliasingQuality.Medium,
            title = stringResource(Res.string.quality_medium),
            icon = { Icon(painterResource(Res.drawable.star), "") },
        ),
        SettingsOption(
            value = AntiAliasingQuality.High,
            title = stringResource(Res.string.quality_high),
            icon = { Icon(painterResource(Res.drawable.hotel_class), "") },
        )
    )

    SettingsScaffold(
        title = { Text(stringResource(Res.string.settings_graphics)) }
    ) {
        SettingsBooleanCard(
            title = { Text(stringResource(Res.string.settings_graphics_fullscreen)) },
            label = { Text(stringResource(Res.string.settings_graphics_fullscreen_description)) },
            icon = { Icon(painterResource(Res.drawable.fullscreen), "") },
            isEnabled = settings.value.graphicsSettings.isFullscreen,
            setIsEnabled = settingsModel::setIsFullscreen,
        )
        SettingsGenericCard(
            title = { Text(stringResource(Res.string.settings_graphics_resolution)) },
            icon = { Icon(painterResource(Res.drawable.monitor), "") },
            label = {
                Text(
                    model.formatResolution(
                        settings.value.graphicsSettings.isFullscreen,
                        settings.value.graphicsSettings.resolutionSettings.isUseDefaultResolution,
                        settings.value.graphicsSettings.resolutionSettings.resolutionWidth,
                        settings.value.graphicsSettings.resolutionSettings.resolutionHeight
                    )
                )
            },
            enabled = !settings.value.graphicsSettings.isFullscreen,
        ) {
            navigator.push(ResolutionSettingsScreen)
        }
        SettingsOptionsCard(
            title = { Text(stringResource(Res.string.settings_graphics_shadows)) },
            icon = { Icon(painterResource(Res.drawable.tonality), "") },
            options = shadowsQualityOptions,
            selectedOption = settings.value.graphicsSettings.shadowsSettings.shadowsQuality,
            onOptionSelected = {
                settingsModel.setShadowsQuality(it)
                settingsModel.setUseShadows(it != ShadowsQuality.Fake)
            },
            label = {
                Text(
                    model.formatShadowsLabel(
                        shadowsQualityOptions.first {
                            it.value == settings.value.graphicsSettings.shadowsSettings.shadowsQuality
                        }
                    )
                )
            }
        )
        SettingsOptionsCard(
            title = { Text(stringResource(Res.string.settings_graphics_anti_aliasing)) },
            icon = { Icon(painterResource(Res.drawable.high_density), "") },
            options = antiAliasingQualityOptions,
            selectedOption = settings.value.graphicsSettings.antiAliasingSettings.antiAliasingQuality,
            onOptionSelected = {
                it?.let { quality ->
                    settingsModel.setAntiAliasingQuality(quality)
                    settingsModel.setUseAntiAliasing(true)
                } ?: GraphicsSettingsScreen.run {
                    settingsModel.setUseAntiAliasing(false)
                }
            },
            label = {
                Text(
                    model.formatAntiAliasingLabel(
                        antiAliasingQualityOptions.first {
                            it.value == settings.value.graphicsSettings.antiAliasingSettings.antiAliasingQuality
                        }
                    )
                )
            }
        )
    }
}
