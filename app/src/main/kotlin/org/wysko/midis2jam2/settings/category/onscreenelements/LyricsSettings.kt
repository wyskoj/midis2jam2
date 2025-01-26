package org.wysko.midis2jam2.settings.category.onscreenelements

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.settings.SettingsKeys

class LyricsSettings(private val settings: Settings) {
    private var _isShowLyrics =
        MutableStateFlow(settings.getBoolean(SettingsKeys.OnScreenElements.Lyrics.IS_SHOW_LYRICS, true))
    val isShowLyrics: StateFlow<Boolean>
        get() = _isShowLyrics

    fun setShowLyrics(isShowLyrics: Boolean) {
        _isShowLyrics.value = isShowLyrics
        settings.putBoolean(SettingsKeys.OnScreenElements.Lyrics.IS_SHOW_LYRICS, isShowLyrics)
    }
}