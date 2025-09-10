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

package org.wysko.midis2jam2.ui.performance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.WorldOneRegular
import midis2jam2.app.generated.resources.forward_10
import midis2jam2.app.generated.resources.play_pause
import midis2jam2.app.generated.resources.replay_10
import midis2jam2.app.generated.resources.videocam
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.Midis2jam2Action

@Composable
fun ControlsCard(
    isAutoCamActive: Boolean,
    isSlideCamActive: Boolean,
    callAction: (Midis2jam2Action) -> Unit,
) {
    val worldOneRegular = FontFamily(Font(Res.font.WorldOneRegular))
    var showCameraControls by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                }
            },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(showCameraControls) {
            CameraControlsModal(callAction, worldOneRegular, isAutoCamActive, isSlideCamActive)
        }
        PrimaryControlsCard(
            callAction,
            showCameraControls,
            onShowCameraControlsChange = { showCameraControls = it }
        )
    }
}

@Composable
private fun PrimaryControlsCard(
    callAction: (Midis2jam2Action) -> Unit,
    showCameraControls: Boolean,
    onShowCameraControlsChange: (Boolean) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .width(48.dp)
        ) {
            FilledIconToggleButton(checked = showCameraControls, onCheckedChange = onShowCameraControlsChange) {
                Icon(
                    painterResource(Res.drawable.videocam),
                    contentDescription = "Toggle camera controls"
                )
            }
            val primaryColor =
                IconButtonDefaults.iconButtonColors().copy(contentColor = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            IconButton(
                onClick = {
                    callAction(Midis2jam2Action.SeekBackward)
                },
                colors = primaryColor
            ) {
                Icon(painterResource(Res.drawable.replay_10), "")
            }

            IconButton(
                onClick = {
                    callAction(Midis2jam2Action.PlayPause)
                },
                colors = primaryColor
            ) {
                Icon(painterResource(Res.drawable.play_pause), "")
            }

            IconButton(
                onClick = {
                    callAction(Midis2jam2Action.SeekForward)
                },
                colors = primaryColor
            ) {
                Icon(painterResource(Res.drawable.forward_10), "")
            }
        }
    }
}

@Composable
private fun CameraControlsModal(
    callAction: (Midis2jam2Action) -> Unit,
    worldOneRegular: FontFamily,
    isAutoCamActive: Boolean,
    isSlideCamActive: Boolean,
) {
    Card {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    FilledTonalIconButton(onClick = {
                        callAction(Midis2jam2Action.MoveToCameraAngle("cam$i"))
                    }) {
                        Text(text = "$i", fontFamily = worldOneRegular)
                    }
                }
                FilledTonalIconButton(
                    onClick = {
                        callAction(Midis2jam2Action.SwitchToAutoCam)
                    },
                    colors = if (!isAutoCamActive) {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    } else {
                        IconButtonDefaults.filledIconButtonColors()
                    }
                ) {
                    Text(text = "A", fontFamily = worldOneRegular)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 4..6) {
                    FilledTonalIconButton(
                        onClick = {
                            callAction(Midis2jam2Action.MoveToCameraAngle("cam$i"))
                        }
                    ) {
                        Text(text = "$i", fontFamily = worldOneRegular)
                    }
                }

                FilledTonalIconButton(
                    onClick = {
                        callAction(Midis2jam2Action.SwitchToSlideCam)
                    },
                    colors = if (!isSlideCamActive) {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    } else {
                        IconButtonDefaults.filledIconButtonColors()
                    }
                ) {
                    Text(text = "S", fontFamily = worldOneRegular)
                }
            }
        }
    }
}
