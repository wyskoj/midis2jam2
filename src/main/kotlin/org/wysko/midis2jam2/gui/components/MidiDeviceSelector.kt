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

package org.wysko.midis2jam2.gui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wysko.midis2jam2.gui.viewmodel.I18n
import java.text.MessageFormat
import javax.sound.midi.MidiDevice

/**
 * A composable function that displays a dropdown menu to select a MIDI device.
 *
 * @param modifier the modifier to be applied to the component.
 * @param devices the list of available MIDI devices.
 * @param selectedDevice the currently selected MIDI device.
 * @param onDeviceSelect the callback function to be invoked when a MIDI device is selected.
 */
@Composable
fun MidiDeviceSelector(
    modifier: Modifier = Modifier,
    devices: List<MidiDevice.Info>,
    selectedDevice: MidiDevice.Info,
    onDeviceSelect: (MidiDevice.Info) -> Unit
) {
    ExposedDropDownMenu(
        modifier = modifier,
        items = devices,
        selectedItem = selectedDevice,
        title = I18n["midi_device"].value,
        displayText = { it.name },
        secondaryText = {
            if (it.vendor == "Unknown vendor") {
                it.description
            } else {
                MessageFormat.format(I18n["midi_device_description"].value, it.description, it.vendor)
            }
        },
        onItemSelected = onDeviceSelect
    )
}
