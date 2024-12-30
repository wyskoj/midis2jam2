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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

/**
 * Displays an exposed dropdown menu.
 *
 * @param modifier The modifier to be applied to the dropdown menu.
 * @param items The list of items to be displayed in the dropdown menu.
 * @param selectedItem The currently selected item in the dropdown menu.
 * @param title The title text to be displayed in the dropdown menu.
 * @param displayText A lambda function to convert the selected item to a display text.
 * @param secondaryText A lambda function to convert the selected item to a secondary text. Can be null.
 * @param onItemSelected A lambda function called when an item is selected in the dropdown menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ExposedDropDownMenu(
    modifier: Modifier = Modifier,
    items: List<T>,
    selectedItem: T,
    title: String,
    displayText: (T) -> String,
    secondaryText: ((T) -> String)?,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val indicatorWidth = (if (expanded) 2 else 1).dp
    val indicatorColor = if (expanded) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface
    val shape = MaterialTheme.shapes.extraSmall.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
    val focusManager = LocalFocusManager.current
    val rotation: Float by animateFloatAsState(if (expanded) 180f else 0f)
    var textFieldSize by remember { mutableStateOf(Size.Zero) }
    val noPropagationInteractionSource = remember { MutableInteractionSource() }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier
    ) {
        Box( // DECORATOR
            Modifier.drawBehind {
                val strokeWidth = indicatorWidth.value * density
                val y = size.height + strokeWidth / 2
                drawLine(
                    indicatorColor, Offset(0f, y), Offset(size.width, y), strokeWidth
                )
            }) {
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(shape).combinedClickable(
                    interactionSource = noPropagationInteractionSource, indication = null, onClick = {
                        expanded = true
                        focusManager.clearFocus()
                    },
                    // Disable the ripple effect when the dropdown is expanded
                    // to prevent propagation of click interaction
                    enabled = !expanded
                ).onGloballyPositioned { textFieldSize = it.size.toSize() },
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = shape
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier.fillMaxHeight().padding(start = 16.dp), verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title, style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = displayText(selectedItem),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "",
                            modifier = Modifier.padding(end = 12.dp).rotate(rotation)
                        )
                    }
                }
            }

        }
        val scope = rememberCoroutineScope()
        Box {
            DropdownMenu(expanded = expanded, onDismissRequest = {
                scope.launch {
                    expanded = false
                }
            }, modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() }).focusable(false)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(onClick = {
                        onItemSelected(item)
                        scope.launch {
                            expanded = false
                        }
                    }, text = {
                        Column(
                            Modifier.padding(top = 16.dp, bottom = 16.dp)
                        ) {
                            Text(
                                displayText(item), style = MaterialTheme.typography.bodyLarge, modifier = Modifier
                            )
                            secondaryText?.invoke(item)?.let {
                                Text(
                                    it, style = MaterialTheme.typography.labelMedium, modifier = Modifier
                                )
                            }
                        }
                    })
                }
            }
        }
    }
}