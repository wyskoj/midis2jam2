package org.wysko.midis2jam2.gui

/**
 * Defines the screens in the application.
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

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
        val outlinedIcon: ImageVector,
        val filledIcon: ImageVector,
        override val uid: Int = i18nKey.hashCode(),
    ): ApplicationScreen()

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
    ): ApplicationScreen()
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
        outlinedIcon = Icons.Outlined.Home,
        filledIcon = Icons.Default.Home,
    )

    val search = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_search",
        outlinedIcon = Icons.Outlined.Search,
        filledIcon = Icons.Default.Search,
    )

    val settings = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_settings",
        outlinedIcon = Icons.Outlined.Settings,
        filledIcon = Icons.Default.Settings,
    )

    val about = ApplicationScreen.ScreenWithTab(
        i18nKey = "tab_about",
        outlinedIcon = Icons.Outlined.Info,
        filledIcon = Icons.Default.Info,
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

    val tabs: List<ApplicationScreen.ScreenWithTab> = listOf(home, search, settings, about)
}