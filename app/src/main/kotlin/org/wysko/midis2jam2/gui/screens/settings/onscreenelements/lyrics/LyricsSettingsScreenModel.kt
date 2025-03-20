package org.wysko.midis2jam2.gui.screens.settings.onscreenelements.lyrics

import cafe.adriel.voyager.core.model.ScreenModel

class LyricsSettingsScreenModel : ScreenModel {
    fun formatLyricsSize(size: Double): String = "${size}x"
}
