package org.wysko.midis2jam2.gui.screens.settings.graphics.shadows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.speed
import midis2jam2.app.generated.resources.tonality
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardRadio
import org.wysko.midis2jam2.settings.AppModel
import org.wysko.midis2jam2.settings.ShadowsQuality
import org.wysko.midis2jam2.settings.category.graphics.ShadowsSettings

object ShadowsSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val isUseShadows by app.graphics.shadows.isUseShadows.collectAsState()
        val shadowsQuality by app.graphics.shadows.shadowsQuality.collectAsState()

        SettingsScreenSkeleton(title = "Shadows") {
            SettingsCardBoolean(
                isChecked = isUseShadows,
                onValueChange = app.graphics.shadows::setIsUseShadows,
                title = "Display shadows",
                icon = painterResource(Res.drawable.tonality),
                description = { "Draw shadows under objects" }
            )
            SettingsCardRadio(
                "Shadows quality",
                icon = painterResource(Res.drawable.speed),
                selectedOption = shadowsQuality,
                options = ShadowsQuality.entries,
                formatOption = ShadowsQuality::name,
                onOptionSelected = app.graphics.shadows::setShadowsQuality,
                enabled = isUseShadows,
                description = {
                    when (isUseShadows) {
                        true -> shadowsQuality.name
                        false -> "Disabled"
                    }
                }
            )
        }
    }
}