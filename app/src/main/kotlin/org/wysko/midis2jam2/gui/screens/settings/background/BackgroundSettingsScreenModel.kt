package org.wysko.midis2jam2.gui.screens.settings.background

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import midis2jam2.app.generated.resources.*
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.north
import midis2jam2.app.generated.resources.south
import midis2jam2.app.generated.resources.west
import org.jetbrains.compose.resources.painterResource
import java.io.File

private val BACKGROUND_IMAGES_FOLDER = File(System.getProperty("user.home"), ".midis2jam2/backgrounds").also {
    it.mkdirs()
}

class BackgroundSettingsScreenModel : ScreenModel {
    private val _availableImages = MutableStateFlow(getAvailableImages())
    val availableImages: StateFlow<List<String>>
        get() = _availableImages

    fun refreshAvailableImages() {
        _availableImages.value = getAvailableImages()
    }

    private fun getAvailableImages(): List<String> =
        BACKGROUND_IMAGES_FOLDER.listFiles()?.map { it.name } ?: emptyList()

    @Composable
    fun directions() = listOf(
        Direction("North", painterResource(Res.drawable.north)),
        Direction("East", painterResource(Res.drawable.east)),
        Direction("South", painterResource(Res.drawable.south)),
        Direction("West", painterResource(Res.drawable.west)),
        Direction("Up", painterResource(Res.drawable.vertical_align_top)),
        Direction("Down", painterResource(Res.drawable.vertical_align_bottom)),
    )

    data class Direction(val cardTitle: String, val icon: Painter)
}