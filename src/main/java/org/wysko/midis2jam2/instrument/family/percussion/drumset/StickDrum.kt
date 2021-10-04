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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue.collectOne
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/**
 * A drum that is hit with a stick.
 */
abstract class StickDrum protected constructor(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) :
    SingleStickInstrument(context, hits) {

    /**
     * The drum model.
     */
    protected lateinit var drum: Spatial

    /**
     * Handles animation and note handling for the drum recoil.
     *
     * @param time  the current time
     * @param delta the amount of time since the last frame update
     */
    protected fun drumRecoil(time: Double, delta: Float) {
        val recoil = collectOne(hits, context, time)
        recoilDrum(drum, recoil != null, recoil?.velocity ?: 0, delta)
    }

    companion object {
        /**
         * How far the drum should travel when hit.
         */
        const val RECOIL_DISTANCE = -2f
    }
}