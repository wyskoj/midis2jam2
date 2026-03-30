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

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsScreenModel : ScreenModel {
    private val _openSoundbanksRequest = MutableStateFlow(false)
    val openSoundbanksRequest: StateFlow<Boolean> = _openSoundbanksRequest

    fun requestOpenSoundbanks() {
        _openSoundbanksRequest.value = true
    }

    fun consumeOpenSoundbanksRequest() {
        _openSoundbanksRequest.value = false
    }

    fun getAvailableLocales(): List<String> = listOf(
        "en",
        "de",
        "es",
        "fi",
        "fr",
        "hi",
        "it",
        "ja",
        "ko",
        "no",
        "pl",
        "ru",
        "th",
        "tl",
        "tr",
        "uk",
        "vi",
        "zh",
    )
}
