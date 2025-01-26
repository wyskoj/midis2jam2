package org.wysko.midis2jam2.gui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions


object SettingsTab : Tab {
    override val key: ScreenKey = "settings"
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(index = 1u, title = "Settings", icon = null)
        }

    @Composable
    override fun Content() {
        Navigator(MainSettingsScreen)
    }
}