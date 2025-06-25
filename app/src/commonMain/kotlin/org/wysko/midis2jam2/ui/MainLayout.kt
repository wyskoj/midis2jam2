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

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.Tab
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.home_fill
import midis2jam2.app.generated.resources.home_outline
import midis2jam2.app.generated.resources.settings_fill
import midis2jam2.app.generated.resources.settings_outline
import midis2jam2.app.generated.resources.tab_home
import midis2jam2.app.generated.resources.tab_settings
import org.wysko.midis2jam2.ui.home.HomeTab
import org.wysko.midis2jam2.ui.settings.SettingsTab

@Composable
expect fun MainLayout()

val tabs: Map<Tab, TabParameters> = mapOf(
    HomeTab to TabParameters(
        name = Res.string.tab_home,
        iconUnfocused = Res.drawable.home_outline,
        iconFocused = Res.drawable.home_fill,
    ),
    SettingsTab to TabParameters(
        name = Res.string.tab_settings,
        iconUnfocused = Res.drawable.settings_outline,
        iconFocused = Res.drawable.settings_fill,
    ),
)
