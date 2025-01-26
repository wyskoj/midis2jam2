package org.wysko.midis2jam2.gui.screens.settings.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.palette
import midis2jam2.app.generated.resources.texture
import midis2jam2.app.generated.resources.wallpaper
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.ColorName
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardCustomForm
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardRadio
import org.wysko.midis2jam2.settings.AppModel
import org.wysko.midis2jam2.settings.category.BackgroundSettings

object BackgroundSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()
        val model = koinScreenModel<BackgroundSettingsScreenModel>()

        val availableImages by model.availableImages.collectAsState()
        val configurationType by app.background.configurationType.collectAsState()

        SettingsScreenSkeleton("Background") {
            SettingsCardRadio(
                "Background type",
                icon = painterResource(Res.drawable.wallpaper),
                selectedOption = configurationType,
                options = BackgroundSettings.ConfigurationType.entries,
                formatOption = BackgroundSettings.ConfigurationType::name,
                onOptionSelected = app.background::setConfigurationType,
                onMenuOpen = model::refreshAvailableImages,
            )
            when (configurationType) {
                BackgroundSettings.ConfigurationType.Default -> {
                    // Nothing to show
                }

                BackgroundSettings.ConfigurationType.RepeatedCubeMap -> {
                    val repeatedCubeMapTexture by app.background.repeatedCubeMapTexture.collectAsState()

                    SettingsCardRadio(
                        "Texture",
                        icon = painterResource(Res.drawable.texture),
                        selectedOption = repeatedCubeMapTexture,
                        options = availableImages,
                        onOptionSelected = app.background::setRepeatedCubeMapTexture,
                        formatOption = { it },
                        onMenuOpen = model::refreshAvailableImages,
                    )
                }

                BackgroundSettings.ConfigurationType.UniqueCubeMap -> {
                    val uniqueCubeMapTextures by app.background.uniqueCubeMapTextures.collectAsState()

                    model.directions().forEachIndexed { index, direction ->
                        SettingsCardRadio(
                            "${direction.cardTitle} texture",
                            icon = direction.icon,
                            selectedOption = uniqueCubeMapTextures[index],
                            options = availableImages,
                            onOptionSelected = {
                                app.background.setUniqueCubeMapTextures(
                                    uniqueCubeMapTextures.toMutableList().apply { set(index, it) })
                            },
                            formatOption = { it },
                            description = {
                                when (uniqueCubeMapTextures[index]) {
                                    "" -> "Not set"
                                    else -> uniqueCubeMapTextures[index]
                                }
                            },
                            onMenuOpen = model::refreshAvailableImages,
                        )
                    }
                }

                BackgroundSettings.ConfigurationType.Color -> {
                    val color by app.background.color.collectAsState()
                    var formColor by remember { mutableStateOf(color) }
                    SettingsCardCustomForm(
                        "Color",
                        description = { ColorName.forColor(color) },
                        icon = painterResource(Res.drawable.palette),
                        enabled = true,
                        dialogContent = {
                            ColorPicker(
                                color = color,
                                setColor = { formColor = it },
                            )
                        },
                        isFormValid = { true },
                        onConfirm = {
                            app.background.setColor(formColor)
                        },
                        extra = {
                            Spacer(Modifier.weight(1f))
                            Box(modifier = Modifier.size(36.dp)) {
                                Surface(
                                    Modifier.fillMaxSize(),
                                    color = Color(color),
                                    shape = MaterialTheme.shapes.medium,
                                    content = {}
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}