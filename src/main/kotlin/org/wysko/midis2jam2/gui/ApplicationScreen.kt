/*
 * Copyright (C) 2025 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.gui

/**
 * Defines the screens in the application.
 */

import midis2jam2.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.wysko.midis2jam2.gui.TabFactory.tabs

/**
 * Represents a screen in the application.
 */
sealed class ApplicationScreen {

    /**
     * Unique identifier for each screen.
     */
    abstract val uid: Int

    /**
     * Represents a screen in this application.
     *
     * @property i18nKey The name of the screen.
     * @property outlinedIcon The outlined icon associated with the screen.
     * @property filledIcon The filled icon associated with the screen.
     */
    data class ScreenWithTab(
        val i18nKey: String,
        val outlinedIcon: DrawableResource,
        val filledIcon: DrawableResource,
        override val uid: Int = i18nKey.hashCode(),
    ) : ApplicationScreen()

    /**
     * Represents a screen in this application.
     *
     * @property name The name of the screen.
     * @property parentScreen The parent screen of this screen.
     */
    data class ScreenWithoutTab(
        val name: String,
        val parentScreen: ScreenWithTab,
        override val uid: Int = name.hashCode(),
    ) : ApplicationScreen()
}


/**
 * A class that represents a factory for creating application tabs.
 *
 * The [TabFactory] class provides a set of predefined application tabs, such as HOME, SEARCH, and SETTINGS.
 *
 * @constructor Creates an instance of the [TabFactory] class.

 * @property tabs A list of all predefined [ApplicationScreen] instances.
 */
object TabFactory {
    val home = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_home",
        outlinedIcon = Res.drawable.home_outline,
        filledIcon = Res.drawable.home_fill,
    )

    val search = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_search",
        outlinedIcon = Res.drawable.search,
        filledIcon = Res.drawable.search,
    )

    val playlist = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_playlist",
        outlinedIcon = Res.drawable.playlist,
        filledIcon = Res.drawable.playlist,
    )

    val settings = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_settings",
        outlinedIcon = Res.drawable.settings_outline,
        filledIcon = Res.drawable.settings_fill,
    )

    val about = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_about",
        outlinedIcon = Res.drawable.info_outline,
        filledIcon = Res.drawable.info_fill,
    )

    val backgroundConfiguration = ApplicationScreen.ScreenWithoutTab(
        name = "tab_background",
        parentScreen = settings,
    )

    val graphicsConfiguration = ApplicationScreen.ScreenWithoutTab(
        name = "tab_graphics",
        parentScreen = settings,
    )

    val soundbankConfiguration = ApplicationScreen.ScreenWithoutTab(
        name = "tab_soundbank",
        parentScreen = settings,
    )

    val synthesizerConfiguration = ApplicationScreen.ScreenWithoutTab(
        name = "tab_synthesizer",
        parentScreen = settings,
    )

    val midiDeviceConfiguration = ApplicationScreen.ScreenWithoutTab(
        name = "tab_midi_device",
        parentScreen = settings,
    )

    val lyricsConfiguration = ApplicationScreen.ScreenWithoutTab(
        name = "tab_lyrics",
        parentScreen = settings,
    )

    val tabs: List<ApplicationScreen.ScreenWithTab> = listOf(home, playlist, search, settings, about)
}