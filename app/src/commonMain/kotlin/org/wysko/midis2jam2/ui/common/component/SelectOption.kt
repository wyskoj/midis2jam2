package org.wysko.midis2jam2.ui.common.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.android
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.util.tintEnabled

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

data class SelectOption<T>(
    val value: T,
    val title: String,
    val label: String? = null,
    val icon: DrawableResource? = null,
)

@Composable
fun UnitRow(
    title: @Composable (() -> Unit),
    label: @Composable (() -> Unit)? = null,
    icon: DrawableResource,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
            ) {
                onClick?.invoke()
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(
                painterResource(icon),
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.tintEnabled(enabled),
            )
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                ProvideTextStyle(
                    MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground.tintEnabled(enabled),
                    )
                ) {
                    title()
                }
                label?.let {
                    ProvideTextStyle(
                        MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.tintEnabled(enabled),
                        )
                    ) {
                        it()
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchRow(
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    title: @Composable (() -> Unit),
    label: @Composable (() -> Unit)? = null,
    icon: DrawableResource,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
            ) {
                onCheckedChange?.invoke(!checked)
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(
                painterResource(icon),
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    title()
                }
                label?.let {
                    ProvideTextStyle(
                        MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        it()
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked,
                null,
                interactionSource = interactionSource,
                modifier = Modifier.semantics {
                    this.toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectRow(
    option: T,
    onOptionSelected: ((T) -> Unit)? = null,
    options: List<SelectOption<T>>,
    title: @Composable (() -> Unit),
    label: @Composable (() -> Unit)? = null,
    icon: DrawableResource? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onExpand: (() -> Unit)? = null,
    description: String? = null,
) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isShowBottomSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
            ) {
                onExpand?.invoke()
                isShowBottomSheet = true
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(
                painterResource(icon ?: options.find { it.value == option }?.icon ?: Res.drawable.android),
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    title()
                }
                ProvideTextStyle(
                    MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    if (label != null) {
                        label.invoke()
                    } else {
                        options.find { it.value == option }?.let { option ->
                            description?.let { description ->
                                Text("$description ∙ ${option.title}")
                            } ?: run {
                                Text(option.title)
                            }
                        }
                    }
                }
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(16.dp))
                trailingIcon.invoke()
            }
        }
    }

    if (isShowBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    isShowBottomSheet = false
                }
            },
            sheetState = sheetState,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LazyColumn {
                    items(options) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected?.invoke(it.value)
                                    scope.launch {
                                        sheetState.hide()
                                        isShowBottomSheet = false
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                it.icon?.let { resource -> Icon(painterResource(resource), null) }
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
        }
    }
}

@Composable
fun CategoryHeader(
    text: String,
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
