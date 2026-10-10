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

/** The steps a performance goes through before it can start, in order. */
enum class LoadingStage {
    /** Parsing the MIDI file. */
    ReadingMidi,

    /** Opening the MIDI device, which for the built-in synthesizer means loading its soundbank. */
    LoadingSoundbank,

    /** Assigning instruments and loading their models. The only stage that reports a fraction. */
    BuildingBand,
}

/** Hears how far a performance has got with loading. Called from whichever thread is doing the work. */
interface ProgressListener {
    /** Loading has moved on to [stage]. */
    fun onLoadingStage(stage: LoadingStage)

    /** How much of [LoadingStage.BuildingBand] is done, from 0 to 1. */
    fun onLoadingProgress(progress: Float)

    /** The performance is loaded and about to show its first frame. */
    fun onReady()
}
