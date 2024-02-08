/*
 * Copyright (C) 2024 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.percussion.drumset

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.PercussionInstrument
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration.Companion.isFakeShadows
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.v3

/**
 * A drum set consists of the [BassDrum], [SnareDrum], [HiHat], [Toms][Tom], and [Cymbals][Cymbal]. This class abstracts
 * the common functionality of these instruments. It also handles the visibility of the drum set.
 *
 * @param context The context to the main class.
 * @param events The events that this drum set is responsible for.
 */
abstract class DrumSet(
    context: Midis2jam2,
    events: List<MidiNoteOnEvent>,
) : PercussionInstrument(context, events.toMutableList()) {
    init {
        if (context.isFakeShadows) {
            geometry += context.assetLoader.fakeShadow("Assets/DrumShadow.obj", "Assets/DrumShadow.png").apply {
                loc = v3(0, 0.01, -80)
            }
        }
    }

    override fun calculateVisibility(time: Double, future: Boolean): Boolean =
        (context.drumSetVisibilityManager.isVisible && context.drumSetVisibilityManager.currentlyVisibleDrumSet == this).also {
            if (!isVisible && it) onEntry()
            if (isVisible && !it) onExit()
        }

    override fun adjustForMultipleInstances(delta: Float): Unit = Unit // The drum set is always in the same place.
}
