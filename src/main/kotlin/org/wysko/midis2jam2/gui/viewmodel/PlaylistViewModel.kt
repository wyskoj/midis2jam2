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
import org.wysko.midis2jam2.starter.configuration.PlaylistConfiguration
import java.io.File

class PlaylistViewModel(
    initialConfiguration: PlaylistConfiguration?,
    override val onConfigurationChanged: (PlaylistConfiguration) -> Unit,
) : ConfigurationViewModel<PlaylistConfiguration> {

    private val _isShuffle = MutableStateFlow<Boolean>(false)
    val isShuffle: StateFlow<Boolean>
        get() = _isShuffle

    private val _playlist = MutableStateFlow<List<File>>(listOf())
    val playlist: StateFlow<List<File>>
        get() = _playlist

    fun setIsShuffle(isShuffle: Boolean) {
        _isShuffle.value = isShuffle
    }

    fun setPlaylist(playlist: List<File>) {
        _playlist.value = playlist
    }

    fun clearPlaylist() {
        _playlist.value = listOf()
    }

    override fun generateConfiguration(): PlaylistConfiguration {
        return PlaylistConfiguration(
            isShuffle = _isShuffle.value,
            playlist = _playlist.value
        )
    }

    override fun applyConfiguration(configuration: PlaylistConfiguration) {
        _isShuffle.value = configuration.isShuffle
        _playlist.value = configuration.playlist
    }

    companion object {
        /** Factory for creating [PlaylistViewModel] instances, loading pre-existing configurations if they exist. */
        fun create(
            onConfigurationChanged: (PlaylistConfiguration) -> Unit = {
                PlaylistConfiguration.preserver.saveConfiguration(it)
            }
        ): PlaylistViewModel = PlaylistViewModel(PlaylistConfiguration.preserver.getConfiguration()) {
            onConfigurationChanged(it)
        }
    }
}