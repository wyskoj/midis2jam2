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

package org.wysko.midis2jam2.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.wysko.midis2jam2.domain.settings.AppSettings

/**
 * The categories settings are grouped into. Each platform decides which rows each page holds
 * (see [settingsPages]); a page that has no rows on a platform is simply not shown there.
 */
enum class SettingsPage(val title: StringResource, val icon: DrawableResource) {
    General(Res.string.settings_general, Res.drawable.tune),
    Graphics(Res.string.settings_graphics, Res.drawable.display_settings),
    Camera(Res.string.settings_camera, Res.drawable.camera_video),
    Instruments(Res.string.settings_instruments, Res.drawable.piano),
    OnScreen(Res.string.settings_on_screen_elements, Res.drawable.browse_activity),
    Controls(Res.string.settings_controls, Res.drawable.keyboard),
    Playback(Res.string.settings_playback, Res.drawable.graphic_eq),
}

/** One row in a [SettingsSection]. [isVisible] is read during composition, so it may read state. */
internal class SettingsEntry(
    val isVisible: () -> Boolean = { true },
    val content: @Composable () -> Unit,
)

/** A titled (or untitled) card of rows. */
internal class SettingsSection(
    val title: StringResource?,
    val entries: List<SettingsEntry>,
)

/** Everything one [SettingsPage] shows on the current platform. */
internal class SettingsPageContent(
    val page: SettingsPage,
    val icon: DrawableResource,
    val summary: StringResource,
    val sections: List<SettingsSection>,
)

internal class SettingsSectionBuilder {
    private val entries = mutableListOf<SettingsEntry>()

    /** Adds a row. */
    fun row(isVisible: () -> Boolean = { true }, content: @Composable () -> Unit) {
        entries += SettingsEntry(isVisible, content)
    }

    fun build(title: StringResource?) = SettingsSection(title, entries.toList())
}

internal class SettingsPageBuilder(
    private val page: SettingsPage,
    private val icon: DrawableResource,
    private val summary: StringResource,
) {
    private val sections = mutableListOf<SettingsSection>()

    /** Adds a card of rows, with a small heading above it when [title] is given. */
    fun section(title: StringResource? = null, build: SettingsSectionBuilder.() -> Unit) {
        sections += SettingsSectionBuilder().apply(build).build(title)
    }

    fun build() = SettingsPageContent(page, icon, summary, sections.toList())
}

internal fun settingsPage(
    page: SettingsPage,
    summary: StringResource,
    icon: DrawableResource = page.icon,
    build: SettingsPageBuilder.() -> Unit,
): SettingsPageContent = SettingsPageBuilder(page, icon, summary).apply(build).build()

/** The pages this platform offers, in display order. */
internal expect fun settingsPages(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
): List<SettingsPageContent>

/** The icon that stands for "follow the device" in the theme picker. */
internal expect val deviceThemeIcon: DrawableResource

/** The rows of a locale picker, which differs by platform. */
@Composable
internal expect fun LocaleSelect(
    selectedLocale: String,
    onSelectLocale: (String) -> Unit,
    availableLocales: List<String>,
)
