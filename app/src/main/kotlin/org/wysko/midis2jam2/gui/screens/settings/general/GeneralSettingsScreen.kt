package org.wysko.midis2jam2.gui.screens.settings.general

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.language
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardRadio
import org.wysko.midis2jam2.settings.AppModel

object GeneralSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<GeneralSettingsScreenModel>()
        val app = koinViewModel<AppModel>()
        val locale by app.general.locale.collectAsState()
        val theme by app.general.theme.collectAsState()

        SettingsScreenSkeleton("General") {
            SettingsCardRadio(
                title = "Language",
                icon = painterResource(Res.drawable.language),
                selectedOption = locale,
                options = app.general.getAvailableLocales(),
                formatOption = { it.displayName },
                app.general::setLocale
            )
            SettingsCardRadio(
                title = "Theme",
                icon = model.getThemeIcon(theme),
                selectedOption = theme,
                options = model.getAvailableThemes(),
                formatOption = { it.name },
                app.general::setTheme
            )
        }
    }
}