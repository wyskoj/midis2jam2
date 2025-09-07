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

package org.wysko.midis2jam2.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel
import org.wysko.midis2jam2.ui.home.AndroidHomeScreenModel
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.ui.settings.SettingsScreenModel

actual val uiModule: Module = module {
    // Tabs
    single<HomeScreenModel> { AndroidHomeScreenModel(get(), get()) }
    single { SettingsModel(get()) }

    // Navigation
    single { NavigationModel() }

    // Settings
    single { SettingsScreenModel }
}
