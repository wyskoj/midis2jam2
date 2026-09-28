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

import java.io.File
import kotlin.time.Duration

/**
 * Where the current (or last) recording is up to.
 */
sealed interface RecordingState {
    /** Nothing has been recorded yet. */
    data object Idle : RecordingState

    /**
     * A recording is being made.
     *
     * @property progress How much is done, from 0 to 1, or `null` while it is still starting.
     * @property remaining About how long is left, or `null` until there's enough to go on.
     */
    data class Rendering(val progress: Float?, val remaining: Duration? = null) : RecordingState

    /** The recording finished and was saved to [file]. */
    data class Finished(val file: File) : RecordingState

    /** The recording was stopped before it finished, and the partial file deleted. */
    data object Cancelled : RecordingState

    /** The recording failed, for the reason in [message]. */
    data class Failed(val message: String) : RecordingState
}
