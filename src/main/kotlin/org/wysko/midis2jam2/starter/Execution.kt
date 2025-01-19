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

package org.wysko.midis2jam2.starter

import org.wysko.midis2jam2.starter.configuration.Configuration
import java.io.File

/**
 * Provides functions to start the app.
 */
object Execution {
    /**
     * Starts the midis2jam2 JME application.
     *
     * @param midiFile The MIDI file to play.
     * @param configurations The configurations for the application.
     * @param onStart Callback function called when the application starts.
     * @param onFinish Callback function called when the application is finished.
     */
    fun start(
        midiFile: File,
        configurations: Collection<Configuration>,
        onStart: () -> Unit,
        onFinish: (e: Throwable?) -> Unit,
    ) {
        onStart()
        val midiPackage = runCatching { MidiPackage.build(midiFile, configurations) }.onFailure {
            onFinish(it)
            return
        }

        with(midiPackage.getOrNull() ?: return) {
            Midis2jam2Application(midiFile, configurations, { onFinish(null) }, sequencer, synthesizer, midiDevice).execute()
        }
    }

    /**
     * Starts the app in playlist mode.
     *
     * @param midiFiles List of MIDI files to play.
     * @param configurations The configurations for the application.
     * @param isShuffle `true` if the song order should be randomized, `false` otherwise.
     * @param onStart Callback function called when the application starts.
     * @param onFinish Callback function called when the application is finished.
     */
    fun playPlaylist(
        midiFiles: List<File>,
        configurations: Collection<Configuration>,
        isShuffle: Boolean,
        onStart: () -> Unit,
        onFinish: (e: Throwable?) -> Unit,
    ) {
        onStart()
        val midiPackage = runCatching { MidiPackage.build(null, configurations) }.onFailure {
            onFinish(it)
            return
        }

        with(midiPackage.getOrNull() ?: return) {
            Midis2jam2PlaylistApplication(
                if (isShuffle) midiFiles.shuffled() else midiFiles,
                configurations,
                { onFinish(null) },
                sequencer,
                synthesizer,
                midiDevice
            ).run {
                applyConfigurations(configurations)
                onStart()
                start()
            }
        }
    }
}
