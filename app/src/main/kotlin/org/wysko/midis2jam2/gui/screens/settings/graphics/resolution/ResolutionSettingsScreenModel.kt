package org.wysko.midis2jam2.gui.screens.settings.graphics.resolution

import cafe.adriel.voyager.core.model.ScreenModel

class ResolutionSettingsScreenModel : ScreenModel {
    fun formatResolution(isDefault: Boolean, resolutionX: Int, resolutionY: Int) = when (isDefault) {
        true -> "Default"
        false -> "$resolutionX × $resolutionY"
    }

    fun validateResolution(resolutionX: String, resolutionY: String): Boolean =
        resolutionX.toIntOrNull()?.let { it > 0 } == true && resolutionY.toIntOrNull()?.let { it > 0 } == true
}