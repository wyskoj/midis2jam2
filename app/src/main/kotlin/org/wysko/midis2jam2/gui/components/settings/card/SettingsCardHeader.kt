package org.wysko.midis2jam2.gui.components.settings.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
internal fun CardHeader(
    icon: Painter,
    title: String,
    description: String,
) {
    CardHeader(
        icon = { Icon(icon, "") },
        title = title,
        description = description,
    )
}

@Composable
internal fun CardHeader(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
) {
    Spacer(Modifier.width(12.dp))
    icon()
    Spacer(Modifier.width(24.dp))
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}