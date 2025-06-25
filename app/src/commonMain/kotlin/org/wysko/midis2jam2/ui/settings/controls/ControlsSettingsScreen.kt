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

package org.wysko.midis2jam2.ui.settings.controls

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.keyboard_lock
import midis2jam2.app.generated.resources.mouse_lock
import midis2jam2.app.generated.resources.settings_controls
import midis2jam2.app.generated.resources.settings_controls_lock_cursor
import midis2jam2.app.generated.resources.settings_controls_lock_cursor_description
import midis2jam2.app.generated.resources.settings_controls_sticky_speed_modifier_keys
import midis2jam2.app.generated.resources.settings_controls_sticky_speed_modifier_keys_false
import midis2jam2.app.generated.resources.settings_controls_sticky_speed_modifier_keys_true
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel

object ControlsSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsModel>()
        val settings = settingsModel.appSettings.collectAsState()

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_controls)) }
        ) {
            SettingsBooleanCard(
                title = { Text(stringResource(Res.string.settings_controls_lock_cursor)) },
                icon = { Icon(painterResource(Res.drawable.mouse_lock), "") },
                label = { Text(stringResource(Res.string.settings_controls_lock_cursor_description)) },
                isEnabled = settings.value.controlsSettings.isLockCursor,
                setIsEnabled = settingsModel::setLockCursorEnabled,
            )
            SettingsBooleanCard(
                title = { Text(stringResource(Res.string.settings_controls_sticky_speed_modifier_keys)) },
                icon = { Icon(painterResource(Res.drawable.keyboard_lock), "") },
                label = {
                    Text(
                        stringResource(
                            when (settings.value.controlsSettings.isSpeedModifierKeysSticky) {
                                true -> Res.string.settings_controls_sticky_speed_modifier_keys_true
                                false -> Res.string.settings_controls_sticky_speed_modifier_keys_false
                            }
                        )
                    )
                },
                isEnabled = settings.value.controlsSettings.isSpeedModifierKeysSticky,
                setIsEnabled = settingsModel::setSpeedModifierKeysSticky,
            )
        }
    }
}