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

package org.wysko.midis2jam2.gui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Defines a simple dropdown menu.
 */
@Suppress("FunctionName")
@Composable
fun SimpleExposedDropDownMenu(
    values: List<String>,
    selectedIndex: Int,
    onChange: (Int) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier,
    backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.BackgroundOpacity),
    shape: Shape = MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize)
) {
    SimpleExposedDropDownMenuImpl(
        values = values,
        selectedIndex = selectedIndex,
        onChange = onChange,
        label = label,
        modifier = modifier,
        backgroundColor = backgroundColor,
        shape = shape,
        decorator = { color, width, content ->
            Box(
                Modifier
                    .drawBehind {
                        val strokeWidth = width.value * density
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color,
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth
                        )
                    }
            ) {
                content()
            }
        }
    )
}

@Suppress("FunctionName")
@Composable
private fun SimpleExposedDropDownMenuImpl(
    values: List<String>,
    selectedIndex: Int,
    onChange: (Int) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier,
    backgroundColor: Color,
    shape: Shape,
    decorator: @Composable (Color, Dp, @Composable () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    val indicatorColor =
        if (expanded) MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)
        else MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.UnfocusedIndicatorLineOpacity)
    val indicatorWidth = (if (expanded) 2 else 1).dp
    val labelColor =
        if (expanded) MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)
        else MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
    val trailingIconColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity)

    val rotation: Float by animateFloatAsState(if (expanded) 180f else 0f)

    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.width(IntrinsicSize.Min)) {
        decorator(indicatorColor, indicatorWidth) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(color = backgroundColor, shape = shape)
                    .onGloballyPositioned { textFieldSize = it.size.toSize() }
                    .clip(shape)
                    .clickable {
                        expanded = !expanded
                        focusManager.clearFocus()
                    }
                    .padding(start = 16.dp, end = 12.dp, top = 7.dp, bottom = 10.dp)
            ) {
                Column(Modifier.padding(end = 32.dp)) {
                    ProvideTextStyle(value = MaterialTheme.typography.caption.copy(color = labelColor)) {
                        label()
                    }
                    Text(
                        text = values[selectedIndex],
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Change",
                    tint = trailingIconColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 4.dp)
                        .rotate(rotation)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
        ) {
            values.forEachIndexed { i, v ->
                val scope = rememberCoroutineScope()
                DropdownMenuItem(
                    onClick = {
                        onChange(i)
                        scope.launch {
                            delay(0)
                            expanded = false
                        }
                    }
                ) {
                    Text(v)
                }
            }
        }
    }
}
