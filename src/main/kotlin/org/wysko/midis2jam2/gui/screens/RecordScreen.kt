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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.components.HelpButton
import org.wysko.midis2jam2.gui.components.TextWithLink
import org.wysko.midis2jam2.gui.viewmodel.I18n
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    midiFileNameWithoutExtension: String,
    isLockRecordButton: Boolean,
    onOpenSettings: () -> Unit,
    onGoBack: () -> Unit,
    onBeginRecording: (
        outputVideoFile: File,
        videoQuality: Int,
        videoFps: Int,
        isLockInputWhileRecording: Boolean,
    ) -> Unit,
) {
    var videoQuality by remember { mutableStateOf(50) }
    var videoFps by remember { mutableStateOf(30) }
    var isLockInputWhileRecording by remember { mutableStateOf(true) }

    Scaffold(
        floatingActionButton = {
            HelpButton()
        },
        topBar = {
            TopAppBar(title = {
                Text("Record", style = typography.headlineSmall)
            }, navigationIcon = {
                IconButton(onClick = onGoBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, I18n["back"].value)
                }
            })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).padding(paddingValues).width(400.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Video will be saved to your Videos folder.")
            Column {
                LabelWithValueIndicator("Video quality", videoQuality)
                Slider(
                    value = videoQuality.toFloat(),
                    onValueChange = { videoQuality = it.toInt() },
                    valueRange = 1f..100f,
                )
            }
            Column {
                LabelWithValueIndicator("Video FPS", videoFps)
                Slider(
                    value = videoFps.toFloat(),
                    onValueChange = { videoFps = it.toInt() },
                    valueRange = 10f..120f,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = CenterVertically,
            ) {
                Checkbox(
                    checked = isLockInputWhileRecording,
                    onCheckedChange = { isLockInputWhileRecording = it },
                )
                Text("Lock input while recording")
            }
            TextWithLink(
                text = "Set video resolution in settings.",
                textToLink = "settings",
                onLinkClick = onOpenSettings,
            )
            ElevatedButton(
                onClick =
                    {
                        onBeginRecording(
                            File("video.mkv").also { println(it.path) },
                            videoQuality,
                            videoFps,
                            isLockInputWhileRecording,
                        )
                    },
                modifier = Modifier.width(160.dp).height(48.dp),
                enabled = !isLockRecordButton,
            ) {
                Icon(painterResource("/ico/dot.svg"), "Record", modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Record")
            }
        }
    }
}

@Composable
private fun LabelWithValueIndicator(
    label: String,
    value: Int,
) {
    Row(
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
        Box(Modifier.background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)) {
            Text(
                value.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            )
        }
    }
}
