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
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.Midi.METRONOME_BELL
import org.wysko.midis2jam2.midi.Midi.METRONOME_CLICK
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/**
 * The metronome has two different pendulums. The first animates [METRONOME_CLICK] and the second animates
 * [METRONOME_BELL].
 *
 * Each pendulum swings to the maximum angle to represent a note on that pendulum. For every other note played on each
 * pendulum, the swing direction inverses. So, if a pendulum just swung to the right, it will swing to the left next.
 *
 * Here's generally how the animation component works (this is one of the more difficult instruments to animate). For
 * each pendulum, on each frame:
 *
 *  1. [Stick.handleStick] is performed on a dummy node ([dummyBellNode] or [dummyClickNode]).
 * This node is invisible.
 *  1. The [Stick.handleStick] method returns the [MidiNoteOnEvent] that the strike was intended
 * for, or null if there was no note played. This is saved to a variable ([flipBellLastStrikeFor] or
 * [flipClickLastStrikeFor]).
 *  1. If [Stick.handleStick] reports that it just struck a note, we check to see if it equals the last
 * [MidiNoteOnEvent]. If it's not, we update that variable and flip a boolean indicating the direction
 * ([flipClick] or [flipBell]).
 *  1. We then set the rotation of the actual pendulum, copying the rotation of the dummy node, or effectively
 * mirroring it if the pendulum needs to swing the other direction.
 */
class Metronome(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The pendulum for [METRONOME_CLICK]. */
    private val clickPendulum: Spatial

    /**
     * The dummy node for [METRONOME_CLICK].
     *
     * @see Metronome
     */
    private val dummyClickNode = Node()

    /**
     * The dummy node for [METRONOME_BELL].
     *
     * @see Metronome
     */
    private val dummyBellNode = Node()

    /** The pendulum for [METRONOME_BELL]. */
    private val bellPendulum: Spatial

    /** List of hits for [METRONOME_BELL]. */
    private val bellHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == METRONOME_BELL } as MutableList<MidiNoteOnEvent>

    /** List of hits for [METRONOME_CLICK]. */
    private val clickHits: MutableList<MidiNoteOnEvent> =
        hits.filter { it.note == METRONOME_CLICK } as MutableList<MidiNoteOnEvent>

    /** Keeps track of which direction [clickPendulum] should swing. */
    private var flipClick = false

    /**
     * Keeps track of the last note the [clickPendulum] hit.
     *
     * @see Metronome
     */
    private var flipClickLastStrikeFor: MidiNoteOnEvent? = null

    /** Keeps track of which direction [clickPendulum] should swing. */
    private var flipBell = false

    /**
     * Keeps track of the last note the [clickPendulum] hit.
     *
     * @see Metronome
     */
    private var flipBellLastStrikeFor: MidiNoteOnEvent? = null

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        /* See class documentation for details */

        /* Animate click pendulum */
        val clickStatus = Stick.handleStick(
            context, dummyClickNode, time, delta, clickHits,
            Stick.STRIKE_SPEED * (30.0 / 50), 30.0, Axis.Z
        )
        if (clickStatus.strikingFor !== flipClickLastStrikeFor && clickStatus.strikingFor != null) {
            flipClickLastStrikeFor = clickStatus.strikingFor
            flipClick = !flipClick
        }
        clickPendulum.localRotation = Quaternion().fromAngles(
            0f,
            0f,
            if (flipClick) clickStatus.rotationAngle * -1 + rad(60.0) else clickStatus.rotationAngle
        )

        /* Animate bell pendulum */
        val bellStatus = Stick.handleStick(
            context, dummyBellNode, time, delta, bellHits,
            Stick.STRIKE_SPEED * (30.0 / 50), 30.0, Axis.Z
        )
        if (bellStatus.strikingFor !== flipBellLastStrikeFor && bellStatus.strikingFor != null) {
            flipBellLastStrikeFor = bellStatus.strikingFor
            flipBell = !flipBell
        }
        bellPendulum.localRotation = Quaternion().fromAngles(
            0f,
            0f,
            if (flipBell) bellStatus.rotationAngle * -1 + rad(60.0) else bellStatus.rotationAngle
        )
    }

    init {

        /* Extract separate hits for the bell and the click */

        /* Load box */
        instrumentNode.attachChild(context.loadModel("MetronomeBox.obj", "Wood.bmp"))

        /* Load each pendulum */
        clickPendulum = context.loadModel("MetronomePendjulum1.obj", "ShinySilver.bmp")
        bellPendulum = context.loadModel("MetronomePendjulum2.obj", "HornSkin.bmp")

        /* Create node for click and position */
        val clickPendulumNode = Node()
        clickPendulumNode.attachChild(clickPendulum)
        clickPendulumNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(-30.0))
        clickPendulumNode.setLocalTranslation(0f, 0f, 1f)

        /* Create node for bell and position */
        val bellPendulumNode = Node()
        bellPendulumNode.attachChild(bellPendulum)
        bellPendulumNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(-30.0))
        bellPendulumNode.setLocalTranslation(0f, 0f, 0.5f)

        /* Attach to instrument */
        instrumentNode.attachChild(clickPendulumNode)
        instrumentNode.attachChild(bellPendulumNode)

        /* Positioning */
        instrumentNode.setLocalTranslation(-20f, 0f, -46f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, rad(20.0), 0f)
    }
}