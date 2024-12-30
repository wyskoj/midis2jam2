/*
 * Copyright (C) 2024 Jacob Wysko
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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.SoundBankConfigurationViewModel
import org.wysko.midis2jam2.starter.configuration.SOUNDBANK_FILE_EXTENSIONS
import java.io.File

@Composable
fun SoundbankConfigurationScreen(
    viewModel: SoundBankConfigurationViewModel,
    onGoBack: () -> Unit,
) {
    val soundbanks by viewModel.soundbanks.collectAsState()
    val soundbankFilesSelectLauncher = rememberFilePickerLauncher(
        mode = PickerMode.Multiple(),
        type = PickerType.File(SOUNDBANK_FILE_EXTENSIONS),
        title = "Select soundbanks",
    ) { files ->
        files?.forEach { file ->
            file.path?.let {
                viewModel.addSoundbank(it)
            }
        }
    }

    Scaffold(
        topBar = { SoundbankTopBar(onGoBack) },
        floatingActionButton = { AddSoundbankFab { soundbankFilesSelectLauncher.launch() } }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(soundbanks) { soundbanks ->
                Column(
                    modifier = Modifier.verticalScroll(state = rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    soundbanks.takeUnless { it.isEmpty() }?.sortedBy { it.parent }?.sortedBy { it.name }
                        ?.forEach { soundbank ->
                            SoundbankCard(soundbank) {
                                viewModel.removeSoundbank(it)
                            }
                        } ?: run {
                        Text(I18n["soundbank_no_soundbanks"].value, fontStyle = FontStyle.Italic)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundbankTopBar(onGoBack: () -> Unit) {
    TopAppBar(title = { Text(I18n["soundbank_configure"].value) }, navigationIcon = {
        IconButton(onClick = onGoBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, I18n["back"].value)
        }
    })
}

@Composable
private fun AddSoundbankFab(showFilePicker: () -> Unit) {
    ExtendedFloatingActionButton(onClick = showFilePicker) {
        Icon(Icons.Default.Add, I18n["soundbank_add"].value)
        Spacer(Modifier.padding(8.dp))
        Text(I18n["soundbank_add"].value)
    }
}

@Composable
private fun SoundbankCard(
    file: File,
    onSoundbankDelete: (File) -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(file.name)
                Text(file.parent, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onSoundbankDelete(file) }) {
                Icon(Icons.Default.Delete, I18n["soundbank_remove"].value)
            }
        }
    }
}