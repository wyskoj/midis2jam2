package org.wysko.midis2jam2

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.TabNavigator
import org.wysko.midis2jam2.ui.MainLayout
import org.wysko.midis2jam2.ui.common.material.AppTheme
import org.wysko.midis2jam2.ui.home.HomeTab

@Composable
fun App() {
    AppTheme {
        TabNavigator(HomeTab) { navigator ->
            MainLayout()
        }
    }
}

