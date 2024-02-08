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

package org.wysko.midis2jam2.gui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.gui.material.AppTheme
import org.wysko.midis2jam2.util.ErrorHandling
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ErrorDialog() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    AppTheme(true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Snackbar(
                                modifier = Modifier.fillMaxWidth(0.5f)
                            ) {
                                Text(it.visuals.message)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                },
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource("/ico/error.svg"), "Error", modifier = Modifier.size(48.dp))
                        Text(ErrorHandling.errorMessage.value)
                    }
                    OutlinedTextField(
                        value = ErrorHandling.errorException.value?.stackTraceToString() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                        textStyle = TextStyle.Default.copy(
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(onClick = {
                            with(
                                StringSelection(
                                    ErrorHandling.errorException.value?.stackTraceToString() ?: ""
                                )
                            ) {
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(this, this)
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("Copied to clipboard")
                            }
                        }) {
                            Text("Copy to clipboard")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { ErrorHandling.dismiss() }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}