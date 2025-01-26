package org.wysko.midis2jam2.gui.screens.settings.graphics.antialiasing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.gradient
import midis2jam2.app.generated.resources.speed
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardRadio
import org.wysko.midis2jam2.settings.AntialiasingQuality
import org.wysko.midis2jam2.settings.AppModel
import org.wysko.midis2jam2.settings.category.graphics.antialiasing.AntialiasingSettings

object AntialiasingSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val isUseAntialiasing by app.graphics.antialiasing.isUseAntialiasing.collectAsState()
        val antialiasingQuality by app.graphics.antialiasing.antialiasingQuality.collectAsState()

        SettingsScreenSkeleton("Antialiasing") {
            SettingsCardBoolean(
                isChecked = isUseAntialiasing,
                onValueChange = app.graphics.antialiasing::setUseAntialiasing,
                title = "Antialiasing",
                icon = painterResource(Res.drawable.gradient),
                description = { "Reduce the appearance of jagged edges" }
            )
            SettingsCardRadio(
                "Antialiasing quality",
                icon = painterResource(Res.drawable.speed),
                selectedOption = antialiasingQuality,
                options = AntialiasingQuality.entries,
                formatOption = AntialiasingQuality::name,
                onOptionSelected = app.graphics.antialiasing::setAntialiasingQuality,
                enabled = isUseAntialiasing,
                description = {
                    when (isUseAntialiasing) {
                        true -> antialiasingQuality.name
                        false -> "Disabled"
                    }
                }
            )
        }
    }
}