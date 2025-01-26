package org.wysko.midis2jam2.gui.screens.settings.graphics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.*
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.fullscreen
import midis2jam2.app.generated.resources.monitor
import midis2jam2.app.generated.resources.tonality
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardNavigate
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardNavigateWithBoolean
import org.wysko.midis2jam2.gui.screens.settings.graphics.antialiasing.AntialiasingSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.graphics.resolution.ResolutionSettingsScreen
import org.wysko.midis2jam2.gui.screens.settings.graphics.resolution.ResolutionSettingsScreenModel
import org.wysko.midis2jam2.gui.screens.settings.graphics.shadows.ShadowsSettingsScreen
import org.wysko.midis2jam2.settings.AppModel

object GraphicsSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val resolutionModel = koinScreenModel<ResolutionSettingsScreenModel>()
        val app = koinViewModel<AppModel>()

        val isFullscreen by app.graphics.isFullscreen.collectAsState()
        val isUseDefaultResolution by app.graphics.resolution.isUseDefaultResolution.collectAsState()
        val resolutionX by app.graphics.resolution.resolutionX.collectAsState()
        val resolutionY by app.graphics.resolution.resolutionY.collectAsState()
        val isUseShadows by app.graphics.shadows.isUseShadows.collectAsState()
        val shadowsQuality by app.graphics.shadows.shadowsQuality.collectAsState()
        val isUseAntialiasing by app.graphics.antialiasing.isUseAntialiasing.collectAsState()
        val antialiasingQuality by app.graphics.antialiasing.antialiasingQuality.collectAsState()

        SettingsScreenSkeleton("Graphics") {
            SettingsCardBoolean(
                title = "Fullscreen",
                icon = painterResource(Res.drawable.fullscreen),
                isChecked = isFullscreen,
                onValueChange = { app.graphics.setIsFullscreen(!isFullscreen) },
                description = { "Use the entire screen" }
            )
            SettingsCardNavigate(
                title = "Resolution",
                icon = painterResource(Res.drawable.monitor),
                description = {
                    when (isFullscreen) {
                        true -> "Resolution matches monitor when fullscreen is enabled"
                        false -> resolutionModel.formatResolution(isUseDefaultResolution, resolutionX, resolutionY)
                    }
                },
                onClick = {
                    navigator.push(ResolutionSettingsScreen)
                },
                enabled = { !isFullscreen }
            )
            SettingsCardNavigateWithBoolean(
                "Shadows",
                icon = painterResource(Res.drawable.tonality),
                description = {
                    when (isUseShadows) {
                        true -> "Enabled • ${shadowsQuality.name}"
                        false -> "Disabled"
                    }
                },
                onNavigate = {
                    navigator.push(ShadowsSettingsScreen)
                },
                onToggle = {
                    app.graphics.shadows.setIsUseShadows(!isUseShadows)
                },
                isChecked = isUseShadows,
            )
            SettingsCardNavigateWithBoolean(
                "Antialiasing",
                icon = painterResource(Res.drawable.gradient),
                description = {
                    when (isUseAntialiasing) {
                        true -> "Enabled • ${antialiasingQuality.name}"
                        false -> "Disabled"
                    }
                },
                onNavigate = {
                    navigator.push(AntialiasingSettingsScreen)
                },
                onToggle = {
                    app.graphics.antialiasing.setUseAntialiasing(!isUseAntialiasing)
                },
                isChecked = isUseAntialiasing,
            )
        }
    }
}