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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.util.hexStringToIntArgb
import org.wysko.midis2jam2.util.hsv

@Composable
fun ColorPicker(
    color: Color,
    setColor: (Int) -> Unit,
    circleSize: Int = 16,
    overallSize: Int = 256,
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var value by remember { mutableStateOf(0f) }

    var hex by remember { mutableStateOf("") }

    // Update the color on-load
    LaunchedEffect(Unit) {
        with(color.toArgb().hsv) {
            hue = first
            saturation = second
            value = third
        }
    }

    val computedColor by derivedStateOf {
        Color.hsv(hue, saturation, value)
    }

    // Update hex when hue, saturation, or value changes
    LaunchedEffect(hue, saturation, value) {
        hex = "#%06X".format(computedColor.toArgb() and 0xFFFFFF)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            Modifier.padding(16.dp),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SaturationValuePicker(overallSize, saturation, value, {
                        saturation = it
                    }, {
                        value = it
                    }, hue, circleSize) {
                        setColor(computedColor.toArgb())
                    }
                    HuePicker(overallSize, hue, {
                        hue = it
                    }) {
                        setColor(computedColor.toArgb())
                    }
                }
                OutlinedTextField(
                    hex,
                    { input ->
                        hex = input
                        hexStringToIntArgb(input)?.let {
                            with(it.hsv) {
                                hue = first
                                saturation = second
                                value = third
                            }
                            setColor(it)
                        }
                    },
                    modifier = Modifier.width(336.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HuePicker(
    size: Int,
    hue: Float,
    setHue: (Float) -> Unit,
    onRelease: () -> Unit = {},
) {
    var mouseDown by remember { mutableStateOf(false) }
    Surface(
        modifier =
            Modifier
                .height(size.dp)
                .width(64.dp)
                .onPointerEvent(
                    PointerEventType.Move,
                ) {
                    if (!mouseDown) return@onPointerEvent
                    val pos = it.changes.first().position
                    val y = pos.y.coerceIn(0f, size.toFloat())
                    setHue(y / size.toFloat() * 360f)
                }.onPointerEvent(
                    PointerEventType.Press,
                ) {
                    mouseDown = true
                    val pos = it.changes.first().position
                    val y = pos.y.coerceIn(0f, size.toFloat())
                    setHue(y / size.toFloat() * 360f)
                }.onPointerEvent(
                    PointerEventType.Release,
                ) {
                    mouseDown = false
                    onRelease()
                },
        color = Color.Gray,
        shape = MaterialTheme.shapes.medium,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..size) {
                drawLine(
                    Color.hsv(
                        i / size.toFloat() * 360f,
                        1f,
                        1f,
                    ),
                    Offset(0f, i.toFloat()),
                    Offset(size.toFloat(), i.toFloat()),
                )
            }
            drawLine(
                Color.White,
                Offset(0f, hue / 360f * size),
                Offset(64f, hue / 360f * size),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SaturationValuePicker(
    size: Int,
    saturation: Float,
    value: Float,
    setSaturation: (Float) -> Unit,
    setValue: (Float) -> Unit,
    hue: Float,
    circleSize: Int,
    onRelease: () -> Unit = {},
) {
    var mouseDown by remember { mutableStateOf(false) }
    Surface(
        modifier =
            Modifier
                .width(size.dp)
                .height(size.dp)
                .onPointerEvent(
                    PointerEventType.Move,
                ) {
                    if (!mouseDown) return@onPointerEvent
                    val pos = it.changes.first().position
                    val x = pos.x.coerceIn(0f, size.toFloat())
                    val y = pos.y.coerceIn(0f, size.toFloat())
                    setSaturation(x / size.toFloat())
                    setValue(1f - y / size.toFloat())
                }.onPointerEvent(
                    PointerEventType.Press,
                ) {
                    mouseDown = true
                    val pos = it.changes.first().position
                    val x = pos.x.coerceIn(0f, size.toFloat())
                    val y = pos.y.coerceIn(0f, size.toFloat())
                    setSaturation(x / size.toFloat())
                    setValue(1f - y / size.toFloat())
                }.onPointerEvent(
                    PointerEventType.Release,
                ) {
                    mouseDown = false
                    onRelease()
                },
        color = Color.Gray,
        shape = MaterialTheme.shapes.medium,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0..size) {
                drawLine(
                    Brush.horizontalGradient(
                        listOf(
                            Color.hsv(hue, 0f, 1f - i / size.toFloat()),
                            Color.hsv(hue, 1f, 1f - i / size.toFloat()),
                        ),
                    ),
                    Offset(0f, i.toFloat()),
                    Offset(size.toFloat(), i.toFloat()),
                )
            }
            drawArc(
                Color.White,
                0f,
                360f,
                false,
                style = Stroke(2f),
                topLeft =
                    Offset(
                        (saturation * size) - circleSize / 2f,
                        (1f - value) * size - circleSize / 2f,
                    ),
                size = Size(circleSize.toFloat(), circleSize.toFloat()),
            )
        }
    }
}
