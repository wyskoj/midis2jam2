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

import com.russhwolf.settings.Settings

/** Remembers whether the app has been launched before, so first-launch guidance is shown only once. */
class OnboardingPersistor(private val settings: Settings = platformSettings(StoreName.Onboarding)) {

    /** Whether no performance has been started yet. */
    val isFirstLaunch: Boolean
        get() = settings.getBoolean(KEY_IS_FIRST_LAUNCH, true)

    /** Records that the app has been launched. */
    fun markLaunched() {
        settings.putBoolean(KEY_IS_FIRST_LAUNCH, false)
    }

    private companion object {
        const val KEY_IS_FIRST_LAUNCH = "isFirstLaunch"
    }
}
