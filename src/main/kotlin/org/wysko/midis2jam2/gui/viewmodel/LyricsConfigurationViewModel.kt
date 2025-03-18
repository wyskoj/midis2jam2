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

package org.wysko.midis2jam2.gui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wysko.midis2jam2.starter.configuration.LyricSize
import org.wysko.midis2jam2.starter.configuration.LyricsConfiguration

/**
 * Represents a ViewModel for lyrics configuration.
 *
 * This class implements the [ConfigurationViewModel] interface for lyrics configuration.
 *
 * @param onConfigurationChanged Callback function that will be called whenever the configuration changes.
 */
class LyricsConfigurationViewModel(
    initConfiguration: LyricsConfiguration? = null,
    override val onConfigurationChanged: (LyricsConfiguration) -> Unit
) : ConfigurationViewModel<LyricsConfiguration> {

    private val _lyricSize = MutableStateFlow(LyricSize(1f))

    /** The resolution of the window. */
    val lyricSize: StateFlow<LyricSize>
        get() = _lyricSize

    /**
     * Sets the lyric size.
     *
     * @param size The size to set for the lyrics.
     */
    fun setLyricsSize(size: LyricSize) {
        _lyricSize.value = size
        onConfigurationChanged(generateConfiguration())
    }

    override fun generateConfiguration(): LyricsConfiguration {
        return LyricsConfiguration(
            lyricSize.value
        )
    }

    override fun applyConfiguration(configuration: LyricsConfiguration) {
        _lyricSize.value = configuration.lyricSize
    }

    init {
        initConfiguration?.let { applyConfiguration(it) }
    }

    companion object {
        /** Factory for creating [LyricsConfigurationViewModel] instances, loading pre-existing configurations if they exist. */
        fun create(
            onConfigurationChanged: (LyricsConfiguration) -> Unit = {
                LyricsConfiguration.preserver.saveConfiguration(it)
            }
        ): LyricsConfigurationViewModel =
            LyricsConfigurationViewModel(LyricsConfiguration.preserver.getConfiguration()) {
                onConfigurationChanged(it)
            }
    }
}