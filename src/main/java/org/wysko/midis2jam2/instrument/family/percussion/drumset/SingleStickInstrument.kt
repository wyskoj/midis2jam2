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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.instrument.family.percussive.Stick.StickStatus
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** Anything that is hit with a stick. */
abstract class SingleStickInstrument protected constructor(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) :
    PercussionInstrument(context, hits) {

    /** The Stick. */
    protected val stick: Spatial

    /** The Stick node. */
    protected val stickNode: Node = Node()

    /**
     * Handles the animation of the stick.
     *
     * @param time  the current time
     * @param delta the amount of time since the last frame update
     * @param hits  the running list of hits
     */
    open fun handleStick(time: Double, delta: Float, hits: MutableList<MidiNoteOnEvent>): StickStatus =
        Stick.handleStick(context, stick, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)


    init {
        stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp").apply {
            stickNode.attachChild(this)
            localRotation = Quaternion().fromAngles(rad(Stick.MAX_ANGLE), 0f, 0f)
        }
        highLevelNode.attachChild(stickNode)
    }
}