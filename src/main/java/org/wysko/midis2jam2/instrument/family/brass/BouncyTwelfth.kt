/*
 * Copyright (C) 2021 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.family.brass

import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained.TwelfthOfOctave

/**
 * A single instance of some visual that "bounces" to visualize (e.g., choir peep, stage horn). Specifically, when the
 * note begins playing, the model rises up into the air, then slowly falls back down for the duration of the note.
 *
 * BouncyTwelfths animate by keeping track of how much time has elapsed since [play] is called, which is
 * passed the number of seconds the twelfth should fall for. This does mean that the rate at which the twelfth falls is
 * not proportional to the current tempo of the MIDI file.
 *
 * If a [play] is called before it is finished falling, the animation resets.
 */
open class BouncyTwelfth : TwelfthOfOctave() {

    override fun play(duration: Double) {
        playing = true
        progress = 0.0
        this.duration = duration
    }

    override fun tick(delta: Float) {
        /* If finished playing */
        if (progress >= 1) {
            playing = false
            progress = 0.0
        }

        /* If still playing */
        if (playing) {
            /* Update animation progress */
            progress += delta / duration
            /* Update visual position */
            val y = (9.5 - 9.5 * progress).coerceAtLeast(0.0).toFloat()
            animNode.setLocalTranslation(0f, y, 0f)
        } else {
            /* Lock to base position if not playing */
            animNode.setLocalTranslation(0f, 0f, 0f)
        }
    }
}