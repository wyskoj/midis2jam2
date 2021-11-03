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
import com.jme3.scene.Spatial.CullHint
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.HighQ.Companion.LASER_LIFE
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis

/**
 * The High Q. Looks like a laser gun. To animate the laser gun, I just used [Stick.handleStick]. The laser that
 * shoots out of the gun is stationary and appears for [LASER_LIFE] seconds.
 */
class HighQ(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** Contains the laser gun. */
    private val gunNode = Node()

    /** The green beam that "shoots" out of the laser gun. */
    private val laser: Spatial

    /** Timer for keeping track of how long the laser has been visible. */
    private var laserShowTime = 0.0

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val stickStatus =
            Stick.handleStick(context, gunNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X)

        /* If the laser gun just fired, show the laser and start the timer */
        if (stickStatus.justStruck()) {
            laser.cullHint = CullHint.Dynamic
            laserShowTime = 0.0
        }

        /* Increment counter */
        laserShowTime += delta.toDouble()

        /* If the counter has surpassed the maximum time, hide the laser */
        if (laserShowTime > LASER_LIFE) {
            laser.cullHint = CullHint.Always
        }
    }

    companion object {
        /** The amount of time the laser should appear for when the laser gun shoots, expressed in seconds. */
        const val LASER_LIFE = 0.05
    }

    init {
        /* Load laser gun */
        gunNode.attachChild(context.loadModel("Zapper.obj", "Zapper.bmp"))
        instrumentNode.attachChild(gunNode)

        /* Load laser */
        laser = context.loadModel("ZapperLaser.obj", "Laser.bmp")
        instrumentNode.attachChild(laser)

        /* Positioning */
        laser.setLocalTranslation(0f, 0f, -14f)
        instrumentNode.setLocalTranslation(-6f, 45f, -74f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, rad(135.0), 0f)

        /* Hide the laser to begin with */
        laser.cullHint = CullHint.Always
    }
}