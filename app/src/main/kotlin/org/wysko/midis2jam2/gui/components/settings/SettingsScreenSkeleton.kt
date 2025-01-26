package org.wysko.midis2jam2.gui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator

@Composable
fun SettingsScreenSkeleton(
    title: String,
    content: @Composable () -> Unit,
) {
    val navigator = LocalNavigator.current!!

    Column(Modifier.padding(top = 16.dp)) {
        IconButton(onClick = { navigator.pop() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}