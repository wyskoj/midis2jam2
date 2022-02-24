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
package org.wysko.midis2jam2.instrument.family.animusic

import com.jme3.collision.CollisionResults
import com.jme3.math.Quaternion
import com.jme3.math.Ray
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser.Companion.SIGMOID_CALCULATOR
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser.SpaceLaserClone
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.cullHint
import org.wysko.midis2jam2.world.Axis
import kotlin.math.exp
import kotlin.math.sin

private const val LASER_HEIGHT = 727.289f

/**
 * The space laser, as made famous by Stick Figures from Animusic.
 *
 * The space laser's main animation component is the [SpaceLaserClone.laserNode]. It rotates clockwise and
 * counter-clockwise depending on the current note played. Each note has a defined rotation. Middle C has a rotation
 * of zero (i.e., the laser is pointing straight up). Notes lower than Middle C rotate counter-clockwise of center
 * and notes higher than Middle C rotate clockwise of center.
 *
 * In between notes, the laser will gradually rotate to meet the target rotation of the next note. Because a MIDI
 * file could have its notes back-to-back (i.e., there is no silence in between notes), the laser would instantly
 * snap to the next target rotation. To combat this, each note is slightly truncated to allow for a short amount of
 * time for the laser to rotate.
 *
 * The specific angle of the laser for each note is calculated by instantiations of [SpaceLaserAngleCalculator]. This
 * class defines an implementation of this as [SIGMOID_CALCULATOR]. Although this is used, other valid
 * implementations should work as well.
 *
 * To signify that a note is playing, the shooter "shoots" out a laser beam. This is done by un-culling
 * [SpaceLaserClone.laserBeam]. It is attached to [SpaceLaserClone.laserNode] which also contains the shooter. This
 * way, the laser beam and the shooter are grouped together.
 *
 * Besides rotating based on the pitch of each note, the laser is "wobbled" slightly. The
 * [SpaceLaserClone.wobbleIntensity] is increased during a note until a certain threshold is reached.
 *
 * The space laser also animates pitch bend and modulation. The intensity of the aforementioned wobble is driven by
 * the modulation controller. Pitch bend turns the laser proportional to the intensity of the bend. For example, if
 * the note playing is Middle C and the pitch bend dictates the sound should be pitched -100 cents, the laser should
 * point in the same direction as B below Middle C.
 */
class SpaceLaser(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: SpaceLaserType) :
    MonophonicInstrument(context, eventList, SpaceLaserClone::class.java, null) {

    private val pitchBendModulationController = PitchBendModulationController(context, eventList)

    private var pitchBendAmount = 0f

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(-22.5f + updateInstrumentIndex(delta) * 15, 0f, 0f)
    }

    @Suppress("unused")
    companion object {
        /** See https://www.desmos.com/calculator/zbmdwg4vcl */
        val SIGMOID_CALCULATOR: SpaceLaserAngleCalculator =
            object : SpaceLaserAngleCalculator {
                override fun angleFromNote(note: Int, pitchBendAmount: Float): Double {
                    val adjNote = note + pitchBendAmount
                    return (-(1 / (1 + exp((-(adjNote - 64) / 16f))) * 208 - 104)).toDouble()
                }
            }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        pitchBendAmount = pitchBendModulationController.tick(time, delta) {
            clones.any { it.isPlaying }
        }
    }

    /** An individual space laser. */
    inner class SpaceLaserClone : Clone(this@SpaceLaser, 0f, Axis.X) {

        /** The current rotation, in degrees. */
        internal var rotation = 0.0

        /** The node that contains the laser pointer and laser. */
        private val laserNode = Node()

        /** The laser beam. */
        internal val laserBeam: Spatial = context.loadModel("SpaceLaserLaser.fbx", "Laser.bmp")

        /** Timer for how long a note has been playing to calculate wobble. */
        private var wobbleTime = 0.0

        /** The current intensity of the wobble. */
        internal var wobbleIntensity = 0.0

        /** Calculates the angles for notes. */
        private val angleCalculator = SIGMOID_CALCULATOR

        /** The shooter. */
        internal val shooter: Spatial = context.loadModel("SpaceLaser.fbx", "ShinySilver.bmp")

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            if (isPlaying) {
                rotation = angleCalculator.angleFromNote((currentNotePeriod ?: return).midiNote, pitchBendAmount)
                if (wobbleTime == 0.0) pitchBendModulationController.resetModulation()
                wobbleTime += delta
                wobbleIntensity = (wobbleTime - 0.1).coerceIn(0.0..0.07)
            } else {
                notePeriods.firstOrNull()?.let {
                    val startTime = it.startTime
                    if (startTime - time <= 1) {
                        val targetPos = angleCalculator.angleFromNote(it.midiNote, pitchBendAmount)
                        if (startTime - time >= delta) {
                            rotation += (targetPos - rotation) / (startTime - time) * delta
                        }
                    }
                }
                wobbleTime = 0.0
            }
            laserNode.localRotation = Quaternion().fromAngles(
                0f,
                0f,
                rad(rotation + sin(50 * wobbleTime) * wobbleIntensity)
            )
            laserBeam.cullHint = isPlaying.cullHint()

            /* Check collision with stage */
            CollisionResults().apply {
                context.stage.collideWith(
                    Ray(
                        laserBeam.worldTranslation,
                        laserBeam.worldRotation.getRotationColumn(1)
                    ), this
                )
            }.also {
                if (it.size() > 0) {
                    it.closestCollision.run {
                        laserBeam.localScale = Vector3f(1f, 1f, distance / LASER_HEIGHT)
                    }
                } else {
                    laserBeam.localScale = Vector3f.UNIT_XYZ
                }
            }
        }

        override fun moveForPolyphony() {
            laserNode.setLocalTranslation(0f, 0f, indexForMoving() * 5f)
        }

        init {
            laserNode.apply {
                attachChild(shooter)
                attachChild(laserBeam)
            }
            highestLevel.attachChild(laserNode)
        }

        override fun toString(): String {
            return super.toString() + buildString {
                append(debugProperty("rotation", rotation.toFloat()))
                append(debugProperty("wobble", wobbleIntensity.toFloat()))
            }
        }
    }

    /** Defines a type of [SpaceLaser]. */
    enum class SpaceLaserType(
        /** The texture file of the laser. */
        val filename: String
    ) {
        /** Saw laser type. */
        SAW("Laser.bmp"),

        /** Square laser type. */
        SQUARE("LaserRed.png")
    }

    init {
        /* Load base */
        val base = context.loadModel("SpaceLaserBase.fbx", "Wood.bmp")
        (base as Node).apply {
            getChild(0).setMaterial(context.reflectiveMaterial("Assets/ShinySilver.bmp"))
            getChild(2).setMaterial(context.unshadedMaterial("Assets/RubberFoot.bmp"))
        }

        instrumentNode.apply {
            attachChild(base)
            setLocalTranslation(0f, 10f, -30f)
        }

        /* Truncate each note period to allow some space for end-to-end notes */
        notePeriods.forEach {
            when {
                it.duration() > 0.4 -> it.endTime -= 0.1
                it.duration() > 0.2 -> it.endTime -= 0.08
                it.duration() > 0.1 -> it.endTime -= 0.05
                it.duration() > 0.05 -> it.endTime -= 0.025
                else -> it.endTime -= 0.02
            }
        }

        clones.forEach {
            it as SpaceLaserClone
            (it.shooter as Node).apply {
                getChild(0).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))
                getChild(1).setMaterial(context.unshadedMaterial("Assets/" + type.filename))
                getChild(2).setMaterial(context.unshadedMaterial("Assets/RubberFoot.bmp"))
            }
            it.laserBeam.setMaterial(context.unshadedMaterial("Assets/" + type.filename))
        }
    }

    override fun toString(): String {
        return super.toString() + buildString {
        }
    }
}