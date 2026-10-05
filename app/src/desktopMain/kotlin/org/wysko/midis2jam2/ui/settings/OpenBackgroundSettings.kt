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
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import org.koin.compose.koinInject
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel
import org.wysko.midis2jam2.ui.home.HomeTab

/**
 * Returns an action that switches to the Settings tab, on the Graphics page where the background is chosen.
 *
 * It is for other screens, such as the dialog that warns about a broken background before playing.
 */
@Composable
fun rememberOpenBackgroundSettings(): () -> Unit {
    val navigationModel = koinInject<NavigationModel>()
    val tabNavigator = LocalTabNavigator.current
    return {
        navigationModel.requestSettingsPage(SettingsPage.Graphics)
        // Matches the navigation rail: leaving Home returns it to its first screen.
        if (tabNavigator.current == HomeTab) HomeTab.resetToRoot()
        tabNavigator.current = SettingsTab
    }
}
