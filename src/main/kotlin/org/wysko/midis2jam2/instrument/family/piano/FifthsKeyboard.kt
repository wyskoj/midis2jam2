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

package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.Instrument
import kotlin.time.Duration

/**
 * A keyboard that glows the key 7 semitones below the currently playing note.
 */
class FifthsKeyboard(context: Midis2jam2, events: MutableList<MidiEvent>, skin: KeyboardSkin) :
    Keyboard(context, events, skin) {

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        keys.forEach { key ->
            key.root.breadthFirstTraversal {
                if (it is Geometry) {
                    it.material.setColor(
                        "GlowColor",
                        glowColor(collector.currentTimedArcs.any { it.note == (key.midiNote + 5).toByte() })
                    )
                }
            }
        }
    }

    private fun glowColor(isGlowing: Boolean): ColorRGBA =
        if (isGlowing) ColorRGBA(0.9f, 0f, 0f, 1f) else ColorRGBA.Black

    override fun findSimilar(): List<Instrument> =
        context.instruments.filterIsInstance<Keyboard>() // We need to include regular Keyboards as similar instruments
}
