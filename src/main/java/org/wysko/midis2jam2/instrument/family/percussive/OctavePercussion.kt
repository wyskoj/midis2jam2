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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent

/**
 * Octave percussion represents instruments that have twelve notes and are hit with drum sticks.
 *
 * @see Agogos
 * @see Woodblocks
 */
abstract class OctavePercussion protected constructor(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    TwelveDrumOctave(context, eventList) {

    /** The nodes for each note. */
    protected val percussionNodes: Array<Node> = Array(12) { Node() }
}