package org.wysko.midis2jam2.gui.screens.settings.onscreenelements.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.format_size
import midis2jam2.app.generated.resources.lyrics
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardCustomForm
import org.wysko.midis2jam2.settings.AppModel

object LyricsSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()
        val model = koinScreenModel<LyricsSettingsScreenModel>()

        val isShowLyrics by app.onScreenElements.lyrics.isShowLyrics.collectAsState()
        val lyricsSize by app.onScreenElements.lyrics.lyricsSize.collectAsState()

        SettingsScreenSkeleton("Lyrics") {
            SettingsCardBoolean(
                isChecked = isShowLyrics,
                onValueChange = app.onScreenElements.lyrics::setShowLyrics,
                title = "Lyrics",
                icon = painterResource(Res.drawable.lyrics),
                description = { "If the song contains lyrics data, display them in real time" }
            )

            var formLyricsSize by remember { mutableStateOf(lyricsSize) }
            SettingsCardCustomForm(
                title = "Lyrics size",
                description = {
                    when (isShowLyrics) {
                        true -> model.formatLyricsSize(lyricsSize)
                        false -> "Disabled"
                    }
                },
                icon = painterResource(Res.drawable.format_size),
                enabled = isShowLyrics,
                dialogContent = {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Slider(
                            value = formLyricsSize.toFloat(),
                            onValueChange = { formLyricsSize = it.toDouble() },
                            valueRange = 0.5f..2.5f,
                            steps = 3,
                        )
                        Text(model.formatLyricsSize(formLyricsSize), style = MaterialTheme.typography.headlineSmall)
                    }
                },
                isFormValid = { true },
                onConfirm = {
                    app.onScreenElements.lyrics.setLyricsSize(formLyricsSize)
                },
                onCancel = {
                    formLyricsSize = lyricsSize
                }
            )
        }
    }
}