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

package org.wysko.midis2jam2.gui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.components.ExposedDropDownMenu
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.LyricsConfigurationViewModel
import org.wysko.midis2jam2.starter.configuration.LyricSize

/**
 * The screen for configuring lyrics settings.
 *
 * @param viewModel The view model for the lyrics configuration.
 * @param onGoBack The callback to be invoked when the user presses the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsConfigurationScreen(
    viewModel: LyricsConfigurationViewModel,
    onGoBack: () -> Unit,
) {
    val lyricSize by viewModel.lyricSize.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text(I18n["lyrics_configure"].value) }, navigationIcon = {
            IconButton(onClick = onGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, I18n["back"].value)
            }
        })
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier.verticalScroll(state = rememberScrollState()).padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LyricsOptions(viewModel, lyricSize)
            }
        }
    }
}

@Composable
private fun LyricsOptions(
    viewModel: LyricsConfigurationViewModel,
    lyricSize: LyricSize,
) {
    createDropDownMenu(
        title = I18n["lyrics_size"].value,
        items = LyricSize.OPTIONS,
        selectedItem = lyricSize,
    ) { viewModel.setLyricsSize(it) }
}

/**
 * Creates a drop-down menu with the specified title, list of items, selected item, and callback for item selection.
 *
 * @param title The title of the drop-down menu.
 * @param items The list of items to display in the drop-down menu.
 * @param selectedItem The currently selected item in the drop-down menu.
 * @param onItemSelected The callback to be invoked when an item is selected in the drop-down menu.
 */
@Composable
private fun <T> createDropDownMenu(
    title: String,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
) {
    Box(Modifier.width(512.dp)) {
        ExposedDropDownMenu(
            modifier = Modifier,
            items = items,
            selectedItem = selectedItem,
            title = title,
            displayText = { it.toString() },
            secondaryText = null,
            onItemSelected,
        )
    }
}
