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
import org.wysko.midis2jam2.instrument.family.percussion.Retexturable
import org.wysko.midis2jam2.instrument.family.percussion.RetextureType
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.Midi
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/** The Snare drum. */
class SnareDrum(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : PercussionInstrument(context, hits),
    Retexturable {

    /** The list of hits for regular notes. */
    private val regularHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == Midi.ACOUSTIC_SNARE || it.note == Midi.ELECTRIC_SNARE } as MutableList<MidiNoteOnEvent>

    /** The list of hits for side sticks. */
    private val sideHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == Midi.SIDE_STICK } as MutableList<MidiNoteOnEvent>

    /** Contains the side stick. */
    private val sideStickNode = Node()

    /** The stick used for regular hits. */
    private val regularStick: Spatial
    override fun tick(time: Double, delta: Float) {
        val regularStickStatus = Stick.handleStick(
            context,
            regularStick,
            time,
            delta,
            regularHits,
            Stick.STRIKE_SPEED,
            Stick.MAX_ANGLE,
            Axis.X
        )
        val sideStickStatus = Stick.handleStick(
            context,
            sideStickNode,
            time,
            delta,
            sideHits,
            Stick.STRIKE_SPEED,
            Stick.MAX_ANGLE,
            Axis.X
        )
        var regVel = 0
        var sideVel = 0
        if (regularStickStatus.justStruck()) {
            regVel = (regularStickStatus.strike ?: return).velocity
        }
        if (sideStickStatus.justStruck()) {
            sideVel = ((sideStickStatus.strike ?: return).velocity * 0.5).toInt()
        }
        val velocity = regVel.coerceAtLeast(sideVel)
        recoilDrum(recoilNode, velocity != 0, velocity, delta)
    }

    /** The geometry of the snare drum. */
    val drum: Spatial = context.loadModel("DrumSet_SnareDrum.obj", "DrumShell_Snare.bmp")

    init {
        drum.apply { recoilNode.attachChild(this) }
        regularStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp")

        val sideStick = regularStick.clone().apply {
            sideStickNode.attachChild(this)
        }

        val regularStickNode: Node = Node().apply {
            attachChild(regularStick)
        }

        recoilNode.run {
            attachChild(regularStickNode)
            attachChild(sideStickNode)
        }

        highLevelNode.run {
            attachChild(recoilNode)
            move(-10.9f, 16f, -72.5f)
            rotate(rad(10.0), 0f, rad(-10.0))
        }

        regularStickNode.run {
            rotate(0f, rad(80.0), 0f)
            move(10f, 0f, 3f)
        }

        sideStick.run {
            setLocalTranslation(0f, 0f, -2f)
            localRotation = Quaternion().fromAngles(0f, rad(-20.0), 0f)
        }

        sideStickNode.setLocalTranslation(-1f, 0.4f, 6f)
    }

    override fun drum(): Spatial = drum
    override fun retextureType(): RetextureType = RetextureType.SNARE
}