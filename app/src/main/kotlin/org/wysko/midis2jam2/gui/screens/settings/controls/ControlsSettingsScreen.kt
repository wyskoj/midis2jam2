package org.wysko.midis2jam2.gui.screens.settings.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.keyboard_lock
import midis2jam2.app.generated.resources.mouse_lock
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.wysko.midis2jam2.gui.components.settings.SettingsScreenSkeleton
import org.wysko.midis2jam2.gui.components.settings.card.SettingsCardBoolean
import org.wysko.midis2jam2.settings.AppModel

object ControlsSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val app = koinViewModel<AppModel>()

        val isLockCursor by app.controls.isLockCursor.collectAsState()
        val isSpeedModifierKeysSticky by app.controls.isSpeedModifierKeysSticky.collectAsState()

        SettingsScreenSkeleton("Controls") {
            SettingsCardBoolean(
                isChecked = isLockCursor,
                onValueChange = app.controls::setIsLockCursor,
                title = "Lock cursor",
                icon = painterResource(Res.drawable.mouse_lock),
                description = { "Lock the cursor to the window" },
            )
            SettingsCardBoolean(
                isChecked = isSpeedModifierKeysSticky,
                onValueChange = app.controls::setIsSpeedModifierKeysSticky,
                title = "Sticky speed modifier keys",
                description = {
                    when (isSpeedModifierKeysSticky) {
                        true -> "Press Ctrl and Shift to change freecam speed"
                        false -> "Hold Ctrl and Shift to change freecam speed"
                    }
                },
                icon = painterResource(Res.drawable.keyboard_lock),
            )
        }
    }
}