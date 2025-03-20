package org.wysko.midis2jam2.gui.components.settings.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.components.settings.RadioWithLabel

@Composable
private fun SettingsCard(
    title: String,
    icon: Painter,
    description: () -> String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    shape: Shape = MaterialTheme.shapes.medium,
    extra: @Composable RowScope.() -> Unit = {},
) {
    SettingsCard(
        title = title,
        icon = { Icon(painter = icon, contentDescription = null) },
        description = description,
        enabled = enabled,
        onClick = onClick,
        shape = shape,
        extra = extra
    )
}

@Composable
private fun SettingsCard(
    title: String,
    icon: @Composable () -> Unit,
    description: () -> String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    shape: Shape = MaterialTheme.shapes.medium,
    extra: @Composable RowScope.() -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(
                when (enabled) {
                    true -> PointerIcon.Hand
                    false -> PointerIcon.Default
                }
            ),
        onClick = onClick,
        enabled = enabled,
        shape = shape
    ) {
        Box(Modifier.fillMaxWidth().height(76.dp)) {
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                CardHeader(icon, title, description())
                extra()
            }
        }
    }
}

@Composable
fun SettingsCardBoolean(
    isChecked: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    icon: Painter,
    description: () -> String,
    enabled: Boolean = true,
) {
    SettingsCard(
        title = title,
        icon = icon,
        description = description,
        enabled = enabled,
        onClick = { onValueChange(!isChecked) }
    ) {
        Spacer(Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = { onValueChange(it) },
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@Composable
fun <T> SettingsCardRadio(
    title: String,
    icon: Painter,
    selectedOption: T,
    options: List<T>,
    formatOption: ((T) -> String)?,
    onOptionSelected: ((T) -> Unit),
    onMenuOpen: (() -> Unit)? = null,
    enabled: Boolean = true,
    description: (() -> String)? = null,
) {
    SettingsCardRadio(
        title = title,
        icon = { Icon(painter = icon, contentDescription = null) },
        selectedOption = selectedOption,
        options = options,
        formatOption = formatOption,
        onOptionSelected = onOptionSelected,
        onMenuOpen = onMenuOpen,
        enabled = enabled,
        description = description
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsCardRadio(
    title: String,
    icon: @Composable () -> Unit,
    selectedOption: T,
    options: List<T>,
    formatOption: ((T) -> String)?,
    onOptionSelected: ((T) -> Unit),
    onMenuOpen: (() -> Unit)? = null,
    enabled: Boolean = true,
    description: (() -> String)? = null,
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val closeDialog = { isDialogOpen = false }
    fun T.format() = formatOption?.let { it(this) } ?: toString()

    SettingsCard(
        title = title,
        icon = icon,
        description = description ?: { selectedOption.format() },
        onClick = {
            isDialogOpen = true
            onMenuOpen?.invoke()
        },
        enabled = enabled
    )

    if (isDialogOpen) {
        BasicAlertDialog(
            onDismissRequest = closeDialog,
            content = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.widthIn(280.dp, 560.dp)
                ) {
                    Column(
                        Modifier.width(IntrinsicSize.Max)
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()

                        val verticalScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .selectableGroup()
                                .heightIn(0.dp, 384.dp)
                                .verticalScroll(verticalScrollState)
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                        ) {
                            options.forEach {
                                RadioWithLabel(
                                    selected = selectedOption == it,
                                    onSelected = {
                                        onOptionSelected(it)
                                        closeDialog()
                                    },
                                    text = it.format()
                                )
                            }
                        }
                        HorizontalDivider(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                                .requiredHeightIn(48.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton({
                                closeDialog()
                            }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsCardNavigate(
    title: String,
    icon: Painter,
    description: () -> String,
    onClick: () -> Unit,
    enabled: () -> Boolean = { true },
) {
    SettingsCard(
        title = title,
        icon = icon,
        description = description,
        onClick = onClick,
        enabled = enabled()
    )
}

@Composable
fun SettingsCardNavigateWithBoolean(
    title: String,
    icon: Painter,
    description: () -> String,
    isChecked: Boolean,
    onNavigate: () -> Unit,
    onToggle: () -> Unit,
    enabled: () -> Boolean = { true },
) {
    SettingsCard(
        title = title,
        icon = icon,
        description = description,
        onClick = onNavigate,
        enabled = enabled()
    ) {
        Spacer(Modifier.weight(1f))
        VerticalDivider(Modifier.padding(end = 16.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCardCustomForm(
    title: String,
    description: (() -> String),
    icon: Painter,
    enabled: Boolean,
    dialogContent: @Composable () -> Unit,
    isFormValid: () -> Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    extra: @Composable RowScope.() -> Unit = {},
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val closeDialog = { isDialogOpen = false }

    SettingsCard(
        title = title,
        icon = icon,
        description = description,
        enabled = enabled,
        onClick = { isDialogOpen = true },
        extra = extra
    )

    if (isDialogOpen) {
        BasicAlertDialog(
            onDismissRequest = closeDialog,
            content = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.widthIn(280.dp, 560.dp)
                ) {
                    Column(
                        Modifier.width(IntrinsicSize.Max)
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        dialogContent()
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                                .requiredHeightIn(48.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton({
                                onCancel()
                                closeDialog()
                            }) {
                                Text("Cancel")
                            }
                            TextButton({
                                onConfirm()
                                closeDialog()
                            }, enabled = isFormValid()) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsCardGroupNavigate(
    title: String,
    description: () -> String,
    icon: Painter,
    listPosition: ListPosition = ListPosition.Other,
    onClick: () -> Unit = {},
) {
    val shape = when (listPosition) {
        ListPosition.First -> MaterialTheme.shapes.medium.copy(
            bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
        )

        ListPosition.Last -> MaterialTheme.shapes.medium.copy(
            topStart = MaterialTheme.shapes.extraSmall.topStart,
            topEnd = MaterialTheme.shapes.extraSmall.topEnd
        )

        ListPosition.Other -> MaterialTheme.shapes.extraSmall
        ListPosition.Only -> MaterialTheme.shapes.medium
    }
    SettingsCard(
        title = title,
        icon = icon,
        description = description,
        onClick = onClick,
        shape = shape
    )
}