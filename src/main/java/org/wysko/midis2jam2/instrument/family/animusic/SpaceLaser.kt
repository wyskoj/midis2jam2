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
package org.wysko.midis2jam2.instrument.family.animusic

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.cullHint
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis
import kotlin.math.exp
import kotlin.math.sin

/** The space laser, as made famous by Stick Figures from Animusic. */
class SpaceLaser(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    MonophonicInstrument(context, eventList, SpaceLaserClone::class.java, null) {

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(indexForMoving(delta) * 15, 0f, 0f)
    }


    inner class SpaceLaserClone : Clone(this@SpaceLaser, 0f, Axis.X) {

        /** The current rotation, in degrees. */
        private var rotation = 0.0

        /** The node that contains the laser pointer and laser. */
        private val laserNode = Node()

        /** The laser beam. */
        private val laserBeam: Spatial

        /** Timer for how long a note has been playing to calculate wobble. */
        private var wobbleTime = 0.0

        /** The current intensity of the wobble. */
        private var wobbleIntensity = 0.0

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            if (isPlaying) {
                val target = angleFromNote(currentNotePeriod!!.midiNote)

                if (rotation < target) {
                    rotation += 4
                    rotation.coerceAtMost(target).also { rotation = it }
                } else if (rotation > target) {
                    rotation -= 5.0
                    rotation.coerceAtLeast(target).also { rotation = it }
                }

                wobbleTime += delta.toDouble()
                wobbleIntensity = 1.0.coerceAtMost(currentNotePeriod!!.startTime - time)
            } else {
                if (notePeriods.isNotEmpty()) {
                    val startTime = notePeriods[0].startTime
                    if (startTime - time <= 1) {
                        val targetPos = angleFromNote(notePeriods[0].midiNote)
                        /* Don't try and slide if the difference is less than delta */
                        if (startTime - time >= delta) {
                            rotation += (targetPos - rotation) / (startTime - time) * delta
                        }
                    }
                }
            }

            laserNode.localRotation = Quaternion().fromAngles(
                0f, 0f,
                rad(rotation + sin(wobbleTime * 50) * wobbleIntensity)
            )

            laserBeam.cullHint = cullHint(isPlaying)
        }

        private fun angleFromNote(note: Int): Double {
            return -(1 / (1 + exp((-(note - 64) / 16f).toDouble())) * 180 - 90)
        }

        override fun moveForPolyphony() {
            laserNode.setLocalTranslation(0f, 0f, indexForMoving() * 5f)
        }

        init {
            /* Load instrument */
            val shooter = context.loadModel("SpaceLaser.fbx", "ShinySilver.bmp")
            (shooter as Node).getChild(1).setMaterial(context.unshadedMaterial("Assets/Laser.bmp"))
            shooter.getChild(0).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))
            laserNode.attachChild(shooter)
            laserBeam = context.loadModel("SpaceLaserLaser.fbx", "Laser.bmp")
            laserNode.attachChild(laserBeam)
            highestLevel.attachChild(laserNode)
        }
    }

    init {
        /* Load base */
        val base = context.loadModel("SpaceLaserBase.fbx", "Wood.bmp")
        val baseNode = base as Node
        baseNode.getChild(0).setMaterial(context.reflectiveMaterial("Assets/ShinySilver.bmp"))
        baseNode.getChild(2).setMaterial(context.unshadedMaterial("Assets/RubberFoot.bmp"))
        instrumentNode.attachChild(base)
        instrumentNode.setLocalTranslation(0f, 10f, -20f)
    }
}