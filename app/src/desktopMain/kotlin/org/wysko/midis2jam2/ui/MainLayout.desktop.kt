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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.tab.Tab
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.home_fill
import midis2jam2.app.generated.resources.home_outline
import midis2jam2.app.generated.resources.info_fill
import midis2jam2.app.generated.resources.info_outline
import midis2jam2.app.generated.resources.playlist
import midis2jam2.app.generated.resources.search
import midis2jam2.app.generated.resources.settings_fill
import midis2jam2.app.generated.resources.settings_outline
import midis2jam2.app.generated.resources.tab_about
import midis2jam2.app.generated.resources.tab_home
import midis2jam2.app.generated.resources.tab_queue
import midis2jam2.app.generated.resources.tab_search
import midis2jam2.app.generated.resources.tab_settings
import org.wysko.midis2jam2.ui.about.AboutTab
import org.wysko.midis2jam2.ui.home.HomeTab
import org.wysko.midis2jam2.ui.queue.QueueTab
import org.wysko.midis2jam2.ui.search.SearchTab
import org.wysko.midis2jam2.ui.settings.SettingsTab
import java.util.Locale

@Composable
actual fun BasicDeviceScaffold(topBar: @Composable (() -> Unit), content: @Composable (() -> Unit)) {
    Scaffold(
        topBar = topBar,
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding)) {
            content()
        }
    }
}

actual val tabs: Map<Tab, TabParameters>
    get() = mapOf(
        HomeTab to TabParameters(
            name = Res.string.tab_home,
            iconUnfocused = Res.drawable.home_outline,
            iconFocused = Res.drawable.home_fill,
        ),
        QueueTab to TabParameters(
            name = Res.string.tab_queue,
            iconUnfocused = Res.drawable.playlist,
            iconFocused = Res.drawable.playlist,
        ),
        SearchTab to TabParameters(
            name = Res.string.tab_search,
            iconUnfocused = Res.drawable.search,
            iconFocused = Res.drawable.search,
        ),
        SettingsTab to TabParameters(
            name = Res.string.tab_settings,
            iconUnfocused = Res.drawable.settings_outline,
            iconFocused = Res.drawable.settings_fill,
        ),
        AboutTab to TabParameters(
            name = Res.string.tab_about,
            iconUnfocused = Res.drawable.info_outline,
            iconFocused = Res.drawable.info_fill,
        ),
    )
