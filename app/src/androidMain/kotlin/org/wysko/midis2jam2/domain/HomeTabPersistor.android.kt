/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.domain

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.mp.KoinPlatformTools

/** On Android, the home tab's state lives in its own shared preferences file. */
actual fun createHomeTabPersistor(): HomeTabPersistor {
    val context = KoinPlatformTools.defaultContext().get().get<Context>()
    return PreferenceBackedHomeTabPersistor(
        SharedPreferencesSettings(
            delegate = context.getSharedPreferences("midis2jam2_home_tab_state", Context.MODE_PRIVATE)
        )
    )
}
