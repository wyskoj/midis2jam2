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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD

/** The amount of time the laser should appear for when the laser gun shoots, expressed in seconds. */
private const val LASER_LIFE = 0.05

/**
 * The High Q.
 *
 * Looks like a laser gun. The laser that shoots out of the gun is stationary and appears for [LASER_LIFE] seconds.
 */
class HighQ(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : AuxiliaryPercussion(context, hits) {
    private val laserGun =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = context.modelD("Zapper.obj", "Zapper.bmp"),
            actualStick = false,
        ).apply {
            setParent(geometry)
        }

    /** The green beam that "shoots" out of the laser gun. */
    private val laserBeam: Spatial =
        context.modelD("ZapperLaser.obj", "Laser.bmp").apply {
            setMaterial(
                context.unshadedMaterial("Laser.bmp").apply {
                    setColor("GlowColor", ColorRGBA.Green)
                },
            )
            shadowMode = RenderQueue.ShadowMode.Off
            cullHint = CullHint.Always // Start hidden
            setLocalTranslation(0f, 0f, -14f)
        }.also {
            geometry.attachChild(it)
        }

    /** Timer for keeping track of how long the laser has been visible. */
    private var laserShowTime = 0.0

    init {
        with(geometry) {
            setLocalTranslation(-6f, 45f, -74f)
            localRotation = Quaternion().fromAngles(0f, rad(135.0), 0f)
        }
    }

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)
        val stickStatus = laserGun.tick(time, delta)

        // If the laser gun just fired, show the laser and start the timer
        stickStatus.strike?.let {
            laserBeam.cullHint = CullHint.Dynamic
            laserShowTime = 0.0
        }

        // Increment counter
        laserShowTime += delta.toDouble()

        // If the counter has surpassed the maximum time, hide the laser
        if (laserShowTime > LASER_LIFE) {
            laserBeam.cullHint = CullHint.Always
        }
    }
}
