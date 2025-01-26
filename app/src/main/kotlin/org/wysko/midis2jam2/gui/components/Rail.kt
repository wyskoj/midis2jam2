package org.wysko.midis2jam2.gui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import org.wysko.midis2jam2.gui.screens.TabIconsCollection
import org.wysko.midis2jam2.gui.screens.home.HomeTab
import org.wysko.midis2jam2.gui.screens.settings.SettingsTab

@Composable
fun Rail() {
    val tabNavigator = LocalTabNavigator.current
    val tabs = listOf(HomeTab, SettingsTab)

    Row {
        NavigationRail {
            Spacer(Modifier.weight(1f))
            tabs.forEach {
                val isTabSelected = tabNavigator.current == it
                NavigationRailItem(
                    selected = isTabSelected,
                    onClick = { tabNavigator.current = it },
                    icon = { Icon(painter = TabIconsCollection[it].getIcon(isTabSelected), "") },
                    label = { Text(it.options.title) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
        }
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp))
    }
}