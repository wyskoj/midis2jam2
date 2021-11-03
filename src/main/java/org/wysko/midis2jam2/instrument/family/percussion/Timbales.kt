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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.Midi.HIGH_TIMBALE
import org.wysko.midis2jam2.midi.Midi.LOW_TIMBALE
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The timbales. */
class Timbales(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** List of hits for the low timbale. */
    private val lowTimbaleHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == LOW_TIMBALE } as MutableList<MidiNoteOnEvent>

    /** List of hits for the high timbale. */
    private val highTimbaleHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == HIGH_TIMBALE } as MutableList<MidiNoteOnEvent>

    /** The Right hand node. */
    private val highStickNode = Node()

    /** The Left hand node. */
    private val lowStickNode = Node()

    /** The Left timbale anim node. */
    private val lowTimbaleAnimNode = Node()

    /** The Right timbale anim node. */
    private val highTimbaleAnimNode = Node()
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val statusLow = Stick.handleStick(
            context,
            lowStickNode,
            time,
            delta,
            lowTimbaleHits,
            Stick.STRIKE_SPEED,
            Stick.MAX_ANGLE,
            Axis.X
        )
        val statusHigh = Stick.handleStick(
            context,
            highStickNode,
            time,
            delta,
            highTimbaleHits,
            Stick.STRIKE_SPEED,
            Stick.MAX_ANGLE,
            Axis.X
        )
        if (statusLow.justStruck()) {
            val strike = statusLow.strike!!
            recoilDrum(lowTimbaleAnimNode, true, strike.velocity, delta)
        } else {
            recoilDrum(lowTimbaleAnimNode, false, 0, delta)
        }
        if (statusHigh.justStruck()) {
            val strike = statusHigh.strike!!
            recoilDrum(highTimbaleAnimNode, true, strike.velocity, delta)
        } else {
            recoilDrum(highTimbaleAnimNode, false, 0, delta)
        }
    }

    init {
        /* Separate hits */

        /* Load timbales */
        val lowTimbale = context.loadModel("DrumSet_Timbale.obj", "DrumShell_Timbale.bmp")
        val highTimbale = context.loadModel("DrumSet_Timbale.obj", "DrumShell_Timbale.bmp")
        highTimbale.setLocalScale(0.75f)

        /* Attach to nodes */
        lowTimbaleAnimNode.attachChild(lowTimbale)
        highTimbaleAnimNode.attachChild(highTimbale)
        val lowTimbaleNode = Node()
        lowTimbaleNode.attachChild(lowTimbaleAnimNode)
        val highTimbaleNode = Node()
        highTimbaleNode.attachChild(highTimbaleAnimNode)
        instrumentNode.attachChild(lowTimbaleNode)
        instrumentNode.attachChild(highTimbaleNode)

        /* Load sticks */
        lowStickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"))
        highStickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"))

        /* Attach & position sticks */
        lowTimbaleAnimNode.attachChild(lowStickNode)
        highTimbaleAnimNode.attachChild(highStickNode)
        lowTimbaleNode.setLocalTranslation(-45.9f, 50.2f, -59.1f)
        lowTimbaleNode.localRotation = Quaternion().fromAngles(rad(32.0), rad(56.6), rad(-2.6))
        highTimbaleNode.setLocalTranslation(-39f, 50.1f, -69.7f)
        highTimbaleNode.localRotation = Quaternion().fromAngles(rad(33.8), rad(59.4), rad(-1.8))
        lowStickNode.setLocalTranslation(0f, 0f, 10f)
        highStickNode.setLocalTranslation(0f, 0f, 10f)
    }
}