package org.wysko.midis2jam2.gui.screens.settings.graphics.resolution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.fit_screen
import midis2jam2.app.generated.resources.responsive_layout
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardCustomForm
import org.wysko.midis2jam2.settings.AppModel

object ResolutionSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<ResolutionSettingsScreenModel>()
        val app = koinViewModel<AppModel>()

        val isUseDefaultResolution by app.graphics.resolution.isUseDefaultResolution.collectAsState()

        SettingsScreenSkeleton("Resolution") {
            SettingsCardBoolean(
                title = "Use default resolution",
                description = { "Default resolution uses most of the screen" },
                icon = painterResource(Res.drawable.fit_screen),
                isChecked = isUseDefaultResolution,
                onValueChange = app.graphics.resolution::setIsUseDefaultResolution
            )
            var formResolutionX by remember { mutableStateOf("") }
            var formResolutionY by remember { mutableStateOf("") }
            SettingsCardCustomForm(
                "Custom resolution",
                {
                    model.formatResolution(
                        isDefault = isUseDefaultResolution,
                        resolutionX = app.graphics.resolution.resolutionX.value,
                        resolutionY = app.graphics.resolution.resolutionY.value
                    )
                },
                icon = painterResource(Res.drawable.responsive_layout),
                enabled = !isUseDefaultResolution,
                dialogContent = {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            formResolutionX,
                            onValueChange = { formResolutionX = it },
                            label = { Text("Width") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            formResolutionY,
                            onValueChange = { formResolutionY = it },
                            label = { Text("Height") },
                            singleLine = true
                        )
                    }
                },
                isFormValid = {
                    model.validateResolution(formResolutionX, formResolutionY)
                },
                onConfirm = {
                    app.graphics.resolution.setResolutionX(formResolutionX.toInt())
                    app.graphics.resolution.setResolutionY(formResolutionY.toInt())
                },
            )

        }
    }
}