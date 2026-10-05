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

package org.wysko.midis2jam2.ui.common.navigation

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.ui.settings.SettingsPage
import java.io.File

class NavigationModel : ScreenModel {
    private var _applyHomeScreenMidiFile = MutableStateFlow<File?>(null)
    val applyHomeScreenMidiFile: StateFlow<File?>
        get() = _applyHomeScreenMidiFile

    fun setApplyHomeScreenMidiFile(file: File?) {
        _applyHomeScreenMidiFile.value = file
    }

    fun clearApplyHomeScreenMidiFile() {
        _applyHomeScreenMidiFile.value = null
    }

    private val _requestedSettingsPage = MutableStateFlow<SettingsPage?>(null)

    /** The settings page another screen has asked to be shown, until the settings screen has shown it. */
    val requestedSettingsPage: StateFlow<SettingsPage?>
        get() = _requestedSettingsPage

    fun requestSettingsPage(page: SettingsPage) {
        _requestedSettingsPage.value = page
    }

    fun clearRequestedSettingsPage() {
        _requestedSettingsPage.value = null
    }
}