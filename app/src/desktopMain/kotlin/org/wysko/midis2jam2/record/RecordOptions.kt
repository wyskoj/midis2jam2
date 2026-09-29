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

import kotlinx.serialization.Serializable

/**
 * What to record, and how.
 *
 * @property outputPath Where the MP4 is written.
 * @property soundbankPath The soundbank Gervill renders the audio with, or `null` for its default.
 */
@Serializable
data class RecordOptions(
    val outputPath: String,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 60,
    val quality: VideoQuality = VideoQuality.High,
    val soundbankPath: String? = null,
) {
    init {
        require(width > 0 && height > 0) { "The resolution must be positive, but was ${width}x$height." }
        require(width % 2 == 0 && height % 2 == 0) { "The resolution must be even, but was ${width}x$height." }
        require(fps in FRAME_RATES) { "The frame rate must be one of $FRAME_RATES, but was $fps." }
    }

    companion object {
        /** The frame rates a recording can be made at. */
        val FRAME_RATES: List<Int> = listOf(24, 25, 30, 50, 60, 120, 144, 240)
    }
}

/**
 * How much the video is compressed, as an H.264 constant rate factor: lower is better and larger.
 */
@Serializable
enum class VideoQuality(val crf: Int) {
    Low(28),
    Medium(23),
    High(18),
    Maximum(12),
}
