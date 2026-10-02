/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.record

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Makes recordings.
 */
interface Recorder {
    /** Whether a performance or recording is running, in which case another can't be started. */
    val isBusy: StateFlow<Boolean>

    /** How the current (or last) recording is going. */
    val recordingState: StateFlow<RecordingState>

    /** Records [midiFile] to video as [options] say. */
    fun startRecording(midiFile: File, options: RecordOptions)

    /** Stops the recording in progress, if there is one. The partial file is deleted. */
    fun cancelRecording()
}
