package org.wysko.midis2jam2.gui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import cafe.adriel.voyager.navigator.tab.Tab
import midis2jam2.app.generated.resources.*
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.home_fill
import midis2jam2.app.generated.resources.home_outline
import midis2jam2.app.generated.resources.settings_outline
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.gui.screens.home.HomeTab
import org.wysko.midis2jam2.gui.screens.settings.SettingsTab

object TabIconsCollection {
    private val icons: Map<Tab, TabIconSet>
        @Composable
        get() = mapOf(
            HomeTab to TabIconSet(
                painterResource(Res.drawable.home_outline),
                painterResource(Res.drawable.home_fill)
            ),
            SettingsTab to TabIconSet(
                painterResource(Res.drawable.settings_outline),
                painterResource(Res.drawable.settings_fill)
            )
        )

    @Composable
    operator fun get(tab: Tab) = icons[tab] ?: error("No icon set found for tab $tab")

    data class TabIconSet(val outlineIcon: Painter, val filledIcon: Painter) {
        fun getIcon(isSelected: Boolean) = if (isSelected) filledIcon else outlineIcon
    }
}