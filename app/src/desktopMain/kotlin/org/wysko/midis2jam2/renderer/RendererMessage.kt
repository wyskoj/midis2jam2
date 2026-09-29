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

package org.wysko.midis2jam2.renderer

import kotlinx.serialization.Serializable

@Serializable
data class RendererMessage(
    val type: String,
    val message: String? = null,
    val stackTrace: String? = null,
    val trackIndex: Int? = null,
    val framesCaptured: Long? = null,
    val expectedFrames: Long? = null,
    val path: String? = null,
) {
    companion object {
        const val RECORD_PROGRESS: String = "RecordProgress"
        const val RECORD_FINISHED: String = "RecordFinished"
        const val RECORD_CANCELLED: String = "RecordCancelled"

        fun finish() = RendererMessage("Finish")
        fun error(message: String, stackTrace: String) = RendererMessage("Error", message, stackTrace)
        fun queueTrackStart(trackIndex: Int) = RendererMessage("QueueTrackStart", trackIndex = trackIndex)

        fun recordProgress(framesCaptured: Long, expectedFrames: Long) =
            RendererMessage(RECORD_PROGRESS, framesCaptured = framesCaptured, expectedFrames = expectedFrames)

        fun recordFinished(path: String) = RendererMessage(RECORD_FINISHED, path = path)
        fun recordCancelled() = RendererMessage(RECORD_CANCELLED)
    }
}
