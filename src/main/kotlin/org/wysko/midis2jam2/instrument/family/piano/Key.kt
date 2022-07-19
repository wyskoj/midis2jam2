/*
 * Copyright (C) 2022 Jacob Wysko
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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument
import org.wysko.midis2jam2.midi.MidiNoteOnEvent

/**
 * A single key on a [KeyedInstrument].
 */
open class Key protected constructor() {

    /** The uppermost node of this key. */
    val keyNode: Node = Node()

    /** Contains geometry for the "up" key. */
    internal val upNode: Node = Node()

    /** Contains geometry for the "down" key. */
    internal val downNode: Node = Node()

    /** Is this key being pressed? */
    val isBeingPressed: Boolean
        get() = currentNote != null

    /** The current note being animated. */
    private var currentNote: MidiNoteOnEvent? = null

    /**
     * Animates the motion of the key.
     *
     * @param delta the amount of time since the last frame update
     */
    open fun tick(delta: Float) {
        currentNote?.let {
            keyNode.localRotation = Quaternion().fromAngles(
                (0.1f * PercussionInstrument.velocityRecoilDampening(it.velocity)).toFloat(),
                0f,
                0f
            )
            downNode.cullHint = Spatial.CullHint.Dynamic
            upNode.cullHint = Spatial.CullHint.Always
        } ?: run {
            val angles = FloatArray(3)
            keyNode.localRotation.toAngles(angles)
            if (angles[0] > 0.0001) {
                keyNode.localRotation = Quaternion(
                    floatArrayOf(
                        (angles[0] - 0.02f * delta * 50).coerceAtLeast(0f), 0f, 0f
                    )
                )
            } else {
                keyNode.localRotation = Quaternion(floatArrayOf(0f, 0f, 0f))
                downNode.cullHint = Spatial.CullHint.Always
                upNode.cullHint = Spatial.CullHint.Dynamic
            }
        }
    }

    /**
     * Signals that this key is being pressed. You can safely call this function more than once during the lifetime of a
     * note.
     */
    fun pressKey(note: MidiNoteOnEvent) {
        currentNote = note
    }

    /**
     * Signals that this key is being released.
     */
    fun releaseKey() {
        currentNote = null
    }
}