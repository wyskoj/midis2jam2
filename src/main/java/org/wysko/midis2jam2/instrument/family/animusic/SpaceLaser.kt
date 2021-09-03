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
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiControlEvent
import org.wysko.midis2jam2.midi.MidiPitchBendEvent
import org.wysko.midis2jam2.util.Utils.cullHint
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis
import kotlin.math.exp
import kotlin.math.sin

/** The space laser, as made famous by Stick Figures from Animusic. */
class SpaceLaser(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: SpaceLaserType) :
    MonophonicInstrument(context, eventList, SpaceLaserClone::class.java, null) {

    private val pitchBends: MutableList<MidiPitchBendEvent>

    private val modulationEvents: MutableList<MidiControlEvent>

    private var pitchBendAmount = 0.0

    private var modulationAmount = 0.0

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(-22.5f + indexForMoving(delta) * 15, 0f, 0f)
    }

    @Suppress("unused")
    companion object {
        /* See https://www.desmos.com/calculator/zbmdwg4vcl */
        val SIGMOID_CALCULATOR: SpaceLaserAngleCalculator =
            object : SpaceLaserAngleCalculator {
                override fun angleFromNote(note: Int, pitchBendAmount: Double): Double {
                    val adjNote = note + pitchBendAmount / (8192f / 12)
                    return -(1 / (1 + exp((-(adjNote - 64) / 16f))) * 208 - 104)
                }
            }
    }

    override fun tick(time: Double, delta: Float) {
        val pitchBends = NoteQueue.collect(pitchBends, context, time)
        for (midiPitchBendEvent in pitchBends) {
            pitchBendAmount = midiPitchBendEvent.value.toDouble() - 8192
        }
        val mods = NoteQueue.collect(this.modulationEvents, context, time)
        for (modEvent in mods) {
            modulationAmount = modEvent.value.toDouble() / 127
        }
        super.tick(time, delta)
    }

    /** Individual space lasers. */
    inner class SpaceLaserClone : Clone(this@SpaceLaser, 0f, Axis.X) {

        /** The current rotation, in degrees. */
        private var rotation = 0.0

        /** The node that contains the laser pointer and laser. */
        private val laserNode = Node()

        /** The laser beam. */
        internal val laserBeam: Spatial

        /** Timer for how long a note has been playing to calculate wobble. */
        private var wobbleTime = 0.0

        /** The current intensity of the wobble. */
        private var wobbleIntensity = 0.0

        /** Calculates the angles for notes. */
        private val angleCalculator = SIGMOID_CALCULATOR

        /** The shooter. */
        internal val shooter: Spatial

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            if (isPlaying) {
                val target = angleCalculator.angleFromNote(currentNotePeriod!!.midiNote, pitchBendAmount)

                rotation = target

                wobbleTime += delta.toDouble()
                wobbleIntensity = ((time - currentNotePeriod!!.startTime) - 0.1).coerceIn(
                    0.0, modulationAmount
                        .coerceAtLeast(0.05)
                )
            } else {
                if (notePeriods.isNotEmpty()) {
                    val startTime = notePeriods[0].startTime
                    if (startTime - time <= 1) {
                        val targetPos = angleCalculator.angleFromNote(notePeriods[0].midiNote, pitchBendAmount)
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

        override fun moveForPolyphony() {
            laserNode.setLocalTranslation(0f, 0f, indexForMoving() * 5f)
        }

        init {
            shooter = context.loadModel("SpaceLaser.fbx", "ShinySilver.bmp")

            laserBeam = context.loadModel("SpaceLaserLaser.fbx", "Laser.bmp")
            laserNode.apply {
                attachChild(shooter)
                attachChild(laserBeam)
            }

            highestLevel.attachChild(laserNode)
        }
    }

    enum class SpaceLaserType(val filename: String) {
        SAW("Laser.bmp"),
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
        for (i in 0 until notePeriods.size - 1) {
            notePeriods[i].apply { if (duration() > 0.05) endTime -= 0.025 }
        }

        pitchBends = eventList
            .filterIsInstance(MidiPitchBendEvent::class.java)
            .toMutableList()

        modulationEvents = eventList
            .filterIsInstance(MidiControlEvent::class.java)
            .filter { e -> e.controlNum == 1 }
            .toMutableList()

        for (clone in clones) {
            clone as SpaceLaserClone

            (clone.shooter as Node).apply {
                getChild(0).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"))
                getChild(1).setMaterial(context.unshadedMaterial("Assets/" + type.filename))
                getChild(2).setMaterial(context.unshadedMaterial("Assets/RubberFoot.bmp"))
            }

            clone.laserBeam.setMaterial(context.unshadedMaterial("Assets/" + type.filename))
        }
    }
}