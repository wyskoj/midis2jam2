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

package org.wysko.midis2jam2.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.arrow_drop_down
import midis2jam2.app.generated.resources.check_circle
import midis2jam2.app.generated.resources.chevron_right
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.SelectOption

/** True when the settings are shown in a narrow window, where controls move below their label. */
internal val LocalSettingsCompact = compositionLocalOf { false }

private val RowHorizontalPadding = 16.dp

/** The outline colour is bright in dark themes, so dividers use a softer version of it. */
@Composable
internal fun settingsDividerColor() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

/** A small uppercase heading with a card of rows beneath it. */
@Composable
internal fun SettingsSectionBlock(section: SettingsSection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        section.title?.let {
            Text(
                text = stringResource(it).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        SettingsCard {
            val visible = section.entries.map { it.isVisible() }
            section.entries.forEachIndexed { index, entry ->
                AnimatedVisibility(
                    visible = visible[index],
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        // Only a row with a visible row above it needs a divider.
                        if (visible.take(index).any { it }) {
                            HorizontalDivider(color = settingsDividerColor())
                        }
                        entry.content()
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

/**
 * The frame every row shares: a title and description on the left, a control on the right.
 *
 * In a compact layout, a control that needs the room goes in [below] instead of [trailing].
 *
 * @param icon Shown before the title, so rows are recognisable at a glance.
 * @param interaction Click or toggle behaviour, applied to the whole row.
 */
@Composable
internal fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: DrawableResource? = null,
    enabled: Boolean = true,
    interaction: Modifier = Modifier,
    below: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(interaction)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(
                start = RowHorizontalPadding,
                end = RowHorizontalPadding,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            trailing?.invoke(this)
        }
        below?.invoke()
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: DrawableResource? = null,
    description: String? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        title = title,
        description = description,
        icon = icon,
        enabled = enabled,
        interaction = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

/** A row that opens something else (a sheet, a screen, another app). */
@Composable
internal fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    icon: DrawableResource? = null,
    description: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
) {
    SettingsRow(
        title = title,
        description = description,
        icon = icon,
        enabled = enabled,
        interaction = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        trailing = {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 220.dp),
                )
            }
            if (showChevron) {
                Icon(
                    painterResource(Res.drawable.chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/**
 * A row that chooses one of several [options].
 *
 * The choice is a dropdown beside the label, or in a compact layout a bottom sheet.
 *
 * @param icon The icon before the title. When omitted, the selected option's icon is used.
 * @param extraTrailing Extra content shown before the control, such as a swatch or a warning.
 */
@Composable
internal fun <T> SettingsChoiceRow(
    title: String,
    selected: T,
    options: List<SelectOption<T>>,
    onSelected: (T) -> Unit,
    icon: DrawableResource? = null,
    description: String? = null,
    enabled: Boolean = true,
    extraTrailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val current = options.find { it.value == selected }
    val rowIcon = icon ?: current?.icon

    if (LocalSettingsCompact.current) {
        var showSheet by remember { mutableStateOf(false) }
        SettingsRow(
            title = title,
            description = description,
            icon = rowIcon,
            enabled = enabled,
            interaction = Modifier.clickable(enabled = enabled, role = Role.Button) { showSheet = true },
            trailing = {
                extraTrailing?.invoke(this)
                Text(
                    text = current?.title.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 220.dp),
                )
                Icon(
                    painterResource(Res.drawable.chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        if (showSheet) {
            ChoiceSheet(
                title = title,
                selected = selected,
                options = options,
                onSelected = onSelected,
                onDismiss = { showSheet = false },
            )
        }
    } else {
        var expanded by remember { mutableStateOf(false) }
        SettingsRow(
            title = title,
            description = description,
            icon = rowIcon,
            enabled = enabled,
            interaction = Modifier.clickable(enabled = enabled, role = Role.Button) { expanded = true },
            trailing = {
                extraTrailing?.invoke(this)
                DropdownChoice(title, selected, options, onSelected, enabled, expanded) { expanded = it }
            },
        )
    }
}

@Composable
private fun <T> DropdownChoice(
    title: String,
    selected: T,
    options: List<SelectOption<T>>,
    onSelected: (T) -> Unit,
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    Box {
        Surface(
            onClick = { onExpandedChange(true) },
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minWidth = 120.dp)
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = options.find { it.value == selected }?.title.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(painterResource(Res.drawable.arrow_drop_down), contentDescription = title)
            }
        }
        // The default menu surface is the same colour as the card it opens over, so the menu gets
        // a brighter container and an outline to stay distinct from it.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            options.forEach { option ->
                val color = if (option.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Unspecified
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            option.title,
                            color = color,
                            textDecoration = if (option.isError) TextDecoration.LineThrough else null,
                        )
                    },
                    leadingIcon = option.icon?.let {
                        { Icon(painterResource(it), contentDescription = null) }
                    },
                    trailingIcon = if (option.value == selected) {
                        {
                            Icon(
                                painterResource(Res.drawable.check_circle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onSelected(option.value)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceSheet(
    title: String,
    selected: T,
    options: List<SelectOption<T>>,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(Modifier.padding(bottom = 16.dp)) {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = option.value == selected, role = Role.RadioButton) {
                            onSelected(option.value)
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    option.icon?.let {
                        Icon(painterResource(it), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        val color = when {
                            option.isError -> MaterialTheme.colorScheme.error
                            option.value == selected -> MaterialTheme.colorScheme.primary
                            else -> Color.Unspecified
                        }
                        Text(
                            option.title,
                            color = color,
                            textDecoration = if (option.isError) TextDecoration.LineThrough else null,
                        )
                        option.label?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = color) }
                    }
                }
            }
        }
    }
}
