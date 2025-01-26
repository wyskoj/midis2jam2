package org.wysko.midis2jam2.gui.screens.settings.onscreenelements.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.lyrics
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.settings.AppModel

object LyricsSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val isShowLyrics by app.onScreenElements.lyrics.isShowLyrics.collectAsState()

        SettingsScreenSkeleton("Lyrics") {
            SettingsCardBoolean(
                isChecked = isShowLyrics,
                onValueChange = app.onScreenElements.lyrics::setShowLyrics,
                title = "Lyrics",
                icon = painterResource(Res.drawable.lyrics),
                description = { "If the song contains lyrics data, display them in real time" }
            )
        }
    }
}