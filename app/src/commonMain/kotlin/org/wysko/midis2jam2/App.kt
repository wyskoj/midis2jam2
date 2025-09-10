package org.wysko.midis2jam2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.tab.TabNavigator
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.ui.MainLayout
import org.wysko.midis2jam2.ui.common.AppEnvironment
import org.wysko.midis2jam2.ui.common.appLocale
import org.wysko.midis2jam2.ui.common.material.AppTheme
import org.wysko.midis2jam2.ui.home.HomeTab

@Composable
fun App() {
    val systemInteractionService = koinInject<SystemInteractionService>()
    AppTheme {
        TabNavigator(HomeTab) {
            AppEnvironment {
                LaunchedEffect(Unit) {
                    appLocale = systemInteractionService.getLocale().language
                }
                MainLayout()
            }
        }
    }
}

