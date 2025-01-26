package org.wysko.midis2jam2.gui.screens.settings.general

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.computer
import midis2jam2.app.generated.resources.dark_mode
import midis2jam2.app.generated.resources.light_mode
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.gui.material.Theme

class GeneralSettingsScreenModel : ScreenModel {
    fun getAvailableThemes(): List<Theme> = Theme.entries

    @Composable
    fun getThemeIcon(theme: Theme) = when(theme) {
        Theme.Light -> painterResource(Res.drawable.light_mode)
        Theme.Dark -> painterResource(Res.drawable.dark_mode)
        Theme.System -> painterResource(Res.drawable.computer)
    }
}