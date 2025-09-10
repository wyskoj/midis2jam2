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

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.tab_settings
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.transition.SlideAndFadeTransition

object SettingsTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(Res.string.tab_settings),
            icon = null,
        )

    @OptIn(ExperimentalVoyagerApi::class)
    @Composable
    override fun Content() {
        Navigator(SettingsScreen) { navigator ->
            SlideAndFadeTransition(navigator) { screen ->
                screen.Content()
            }
        }
    }
}
