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
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.mp.KoinPlatformTools

/** On Android, each store is its own shared preferences file, under the names earlier versions already use. */
actual fun platformSettings(store: StoreName): Settings {
    val context = KoinPlatformTools.defaultContext().get().get<Context>()
    val fileName = when (store) {
        StoreName.AppSettings -> "midis2jam2_settings"
        StoreName.HomeTab -> "midis2jam2_home_tab_state"
        StoreName.PlaybackHistory -> "midis2jam2_playback_history"
        StoreName.Onboarding -> "org.wysko.midis2jam2.PREFERENCE_FILE_KEY"
    }
    return SharedPreferencesSettings(delegate = context.getSharedPreferences(fileName, Context.MODE_PRIVATE))
}
