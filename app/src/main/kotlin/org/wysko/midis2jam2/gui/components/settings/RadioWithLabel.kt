package org.wysko.midis2jam2.gui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RadioWithLabel(
    selected: Boolean,
    onSelected: () -> Unit,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                onClick = onSelected,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelected,
            modifier = Modifier,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}