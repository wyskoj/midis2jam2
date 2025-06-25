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

@file:OptIn(ExperimentalMaterial3Api::class)

package org.wysko.midis2jam2.ui.common.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun <T> SettingsOptionsCard(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    options: List<SettingsOption<T>>,
    selectedOption: T,
    noOptionsHint: @Composable () -> Unit = {},
    preamble: @Composable (() -> Unit)? = null,
    onOpen: () -> Unit = {},
    label: @Composable (() -> Unit)? = null,
    onOptionSelected: (T) -> Unit = {},
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    SettingsCard({
        showBottomSheet = true
        onOpen()
    }) {
        icon()
        Column {
            ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)) {
                title()
            }
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                label?.invoke() ?: run {
                    options.find { it.value == selectedOption }?.let { selected ->
                        Text(selected.title)
                    }
                }
            }
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet = false
                }
            },
            sheetState = sheetState,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                preamble?.let {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier.padding(horizontal = 16.dp).align(Alignment.CenterHorizontally),
                        ) {
                            it()
                        }
                        HorizontalDivider()
                    }
                }
                LazyColumn {
                    when (options.size) {
                        0 -> item {
                            Column(
                                Modifier.padding(horizontal = 16.dp),
                            ) {
                                noOptionsHint()
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        else -> items(options) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOptionSelected(it.value)
                                        scope.launch {
                                            sheetState.hide()
                                            showBottomSheet = false
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    it.icon?.invoke()
                                    Column {
                                        Text(it.title)
                                        ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                                            it.label?.let { label -> Text(label) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // Fading edge at bottom TODO
//                Box(
//                    Modifier
//                        .align(Alignment.BottomCenter)
//                        .fillMaxWidth()
//                        .height(24.dp)
//                        .background(
//                            Brush.verticalGradient(
//                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
//                            )
//                        )
//                )
            }
        }
    }
}

data class SettingsOption<T>(
    val value: T,
    val title: String,
    val label: String? = null,
    val icon: (@Composable () -> Unit)? = null,
)