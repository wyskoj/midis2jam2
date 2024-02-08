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
package org.wysko.midis2jam2.instrument.family.animusic

import com.jme3.collision.CollisionResults
import com.jme3.math.Ray
import com.jme3.renderer.queue.RenderQueue.ShadowMode.Off
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser.SpaceLaserClone
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.cullHint
import org.wysko.midis2jam2.util.get
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.material
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelD
import kotlin.math.sin

private const val LASER_HEIGHT = 727.289f

/**
 * The space laser, as made famous by Stick Figures from Animusic.
 *
 * The space laser's main animation component is the [SpaceLaserClone.laserNode]. It rotates clockwise and
 * counter-clockwise depending on the current note played. Each note has a defined rotation. Middle C has a rotation
 * of zero (i.e., the laser is pointing straight up). Notes lower than Middle C rotate counter-clockwise of the center
 * and notes higher than Middle C rotate clockwise of the center.
 *
 * In between notes, the laser will gradually rotate to meet the target rotation of the next note. Because a MIDI
 * file could have its notes back-to-back (i.e., there is no silence in between notes), the laser would instantly
 * snap to the next target rotation. To combat this, each note is slightly truncated to allow for a short amount of
 * time for the laser to rotate.
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
 * the note playing is Middle C and the pitch bend dictates that the sound should be pitched -100 cents, the laser
 * should point in the same direction as B below Middle C.
 *
 * @param context The [Midis2jam2] instance.
 * @param eventList The list of [MidiChannelSpecificEvent]s.
 * @param type The type of space laser.
 */
class SpaceLaser(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: SpaceLaserType) :
    MonophonicInstrument(context, eventList, SpaceLaserClone::class, null) {
    override val pitchBendModulationController: PitchBendModulationController = PitchBendModulationController(
        context,
        eventList,
        smoothness = 13.0 // Make laser bend a little more snappy
    )

    private var pitchBendAmount = 0f

    override fun adjustForMultipleInstances(delta: Float) {
        root.loc = v3(-22.5 + updateInstrumentIndex(delta) * 15, 0, 0)
    }

    override fun handlePitchBend(time: Double, delta: Float) {
        pitchBendAmount = pitchBendModulationController.tick(time, delta) {
            clones.any { it.isPlaying }
        }
    }

    init {
        val base = context.modelD("SpaceLaserBase.obj", "Wood.bmp")
        (base as Node).apply {
            this[1].material = context.reflectiveMaterial("Assets/ShinySilver.bmp")
            this[2].material = context.unshadedMaterial("Assets/RubberFoot.bmp")
        }

        with(geometry) {
            +base
            loc = v3(0, 10, -30)
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
            val glowMaterial = context.unshadedMaterial("Assets/" + type.filename).apply {
                setColor("GlowColor", type.glowColor)
            }
            with(it.shooter as Node) {
                this[0].material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp")
                this[1].material = context.unshadedMaterial("Assets/RubberFoot.bmp")
                this[2].material = glowMaterial
            }
            it.laserBeam.material = glowMaterial
        }
    }

    /** An individual space laser. */
    inner class SpaceLaserClone : Clone(this@SpaceLaser, 0f, Axis.X) {

        private var rotation = 0.0
        private val laserNode = with(highestLevel) { +node() }
        private var wobbleTime = 0.0
        internal val laserBeam: Spatial = with(laserNode) {
            +context.modelD("SpaceLaserLaser.obj", "Laser.bmp").apply {
                shadowMode = Off
            }
        }
        private var wobbleIntensity = 0.0
        private val angleCalculator = SigmoidAngleCalculator
        internal val shooter: Spatial = with(laserNode) {
            +context.modelD("SpaceLaser.obj", "ShinySilver.bmp")
        }
        private val index: NumberSmoother = NumberSmoother(0f, 17.0)

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)

            currentNotePeriod?.run {
                adjustPlayingRotation(this, delta)
                scaleByStageCollision()
            } ?: run { adjustIdleRotation(time, delta) }

            laserNode.rot = v3(0, 0, rotation + sin(50 * wobbleTime) * wobbleIntensity)

            // Only show laser beam if currently playing
            laserBeam.cullHint = isPlaying.cullHint()
        }

        private fun adjustPlayingRotation(np: NotePeriod, delta: Float) {
            // Currently playing, so set the correct rotation
            rotation = angleCalculator.angleFromNote(np.midiNote, pitchBendAmount)

            // If just starting playing, reset modulation so that we start with no vibrato
            if (wobbleTime == 0.0) {
                pitchBendModulationController.resetModulation()
            }
            wobbleTime += delta

            // Start wobbling 0.1 secs after starting playing
            wobbleIntensity = (wobbleTime - 0.1).coerceIn(0.0..0.07)
        }

        private fun adjustIdleRotation(time: Double, delta: Float) {
            // Not yet playing. Look ahead to the next NotePeriod
            notePeriodCollector.peek()?.let {
                val startTime = it.startTime
                if (startTime - time <= 1) { // Less than 1 second away from playing
                    val targetPos = angleCalculator.angleFromNote(it.midiNote, pitchBendAmount)
                    if (startTime - time >= delta) {
                        // Slowly inch our way to the target rotation
                        rotation += (targetPos - rotation) / (startTime - time) * delta
                    }
                }
            }
            wobbleTime = 0.0
        }

        private fun scaleByStageCollision() = CollisionResults().apply {
            context.stage.collideWith(
                Ray(laserBeam.worldTranslation, laserBeam.worldRotation.getRotationColumn(1)),
                this
            )
        }.run {
            laserBeam.localScale = v3(1, closestCollision?.let { it.distance / LASER_HEIGHT } ?: 1f, 1)
        }

        override fun indexForMoving(): Float {
            val myIndex = parent.clones.indexOf(this)
            val visible = parent.clones.count { it.isVisible }
            return -0.5f * visible - 1 + myIndex
        }

        override fun adjustForPolyphony(delta: Float) {
            laserNode.loc = v3(0, 0, index.tick(delta) { indexForMoving() * 5f } + 6)
        }

        override fun toString(): String {
            return super.toString() + buildString {
                append(debugProperty("rotation", rotation.toFloat()))
                append(debugProperty("wobble", wobbleIntensity.toFloat()))
            }
        }
    }
}
