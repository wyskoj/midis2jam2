/*
 * Copyright (C) 2023 Jacob Wysko
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

package org.wysko.midis2jam2.gui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wysko.midis2jam2.gui.viewmodel.I18n
import java.io.File

/**
 * A composable function that displays a drop-down menu for selecting a soundbank.
 *
 * @param modifier The modifier to be applied to the component.
 * @param selectedSoundbank The currently selected soundbank, can be null.
 * @param soundbanks The collection of available soundbanks.
 * @param onSoundbankSelect The callback function for when a soundbank is selected.
 */
@Composable
fun SoundbankSelector(
    modifier: Modifier = Modifier,
    selectedSoundbank: File?,
    soundbanks: Collection<File>,
    onSoundbankSelect: (File?) -> Unit
) {
    ExposedDropDownMenu(
        modifier = modifier,
        items = (listOf(null) + soundbanks).toList(),
        selectedItem = selectedSoundbank,
        title = I18n["soundbank"].value,
        displayText = { it?.nameWithoutExtension ?: I18n["soundbank_default"].value },
        secondaryText = { it?.parent ?: I18n["soundbank_default_description"].value },
        onItemSelected = onSoundbankSelect
    )
}