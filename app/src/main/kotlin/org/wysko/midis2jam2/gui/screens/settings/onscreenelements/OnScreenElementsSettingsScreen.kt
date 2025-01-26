package org.wysko.midis2jam2.gui.screens.settings.onscreenelements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.browse_activity
import midis2jam2.app.generated.resources.lyrics
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardNavigateWithBoolean
import org.wysko.midis2jam2.gui.screens.settings.onscreenelements.lyrics.LyricsSettingsScreen
import org.wysko.midis2jam2.settings.AppModel

object OnScreenElementsSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val app = koinViewModel<AppModel>()

        val isShowHeadsUpDisplay by app.onScreenElements.isShowHeadsUpDisplay.collectAsState()
        val isShowLyrics by app.onScreenElements.lyrics.isShowLyrics.collectAsState()

        SettingsScreenSkeleton("On-screen elements") {
            SettingsCardBoolean(
                isChecked = isShowHeadsUpDisplay,
                onValueChange = app.onScreenElements::setShowHeadsUpDisplay,
                title = "Heads-up display",
                icon = painterResource(Res.drawable.browse_activity),
                description = { "Display song name and progress" }
            )
            SettingsCardNavigateWithBoolean(
                title = "Lyrics",
                icon = painterResource(Res.drawable.lyrics),
                description = { if (isShowLyrics) "Enabled" else "Disabled" },
                isChecked = isShowLyrics,
                onNavigate = {
                    navigator.push(LyricsSettingsScreen)
                },
                onToggle = { app.onScreenElements.lyrics.setShowLyrics(!isShowLyrics) }
            )
        }
    }
}