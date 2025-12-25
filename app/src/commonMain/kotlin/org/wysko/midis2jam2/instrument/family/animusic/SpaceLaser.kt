/*
 * Copyright (C) 2025 Jacob Wysko
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
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.manager.StageManager.Companion.stageManager
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.modelD
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

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
 * @param context The [PerformanceManager] instance.
 * @param eventList The list of [MidiEvent]s.
 * @param type The type of space laser.
 */
class SpaceLaser(context: PerformanceManager, eventList: List<MidiEvent>, type: SpaceLaserType) :
    MonophonicInstrument(context, eventList, SpaceLaserClone::class, null) {
    override val pitchBendModulationController: PitchBendModulationController = PitchBendModulationController(
        context,
        eventList,
        smoothness = 13.0 // Make laser bend a little more snappy
    )

    private var pitchBendAmount = 0f

    private val earlyReleases = timedArcs.associateWith {
        when {
            it.duration > 0.4.seconds -> 0.1.seconds
            it.duration > 0.2.seconds -> 0.08.seconds
            it.duration > 0.1.seconds -> 0.05.seconds
            it.duration > 0.05.seconds -> 0.025.seconds
            else -> 0.02.seconds
        }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        root.loc = v3(-22.5 + updateInstrumentIndex(delta) * 15, 0, 0)
    }

    override fun handlePitchBend(time: Duration, delta: Duration, isNewNote: Boolean) {
        pitchBendAmount = pitchBendModulationController.tick(time, delta, isNewNote = isNewNote) {
            collector.currentTimedArcs.isNotEmpty()
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        val isNewNote = collector.advance(time).hasChanged
        isVisible = calculateVisibility(time)
        adjustForMultipleInstances(delta)
        handlePitchBend(time, delta, isNewNote)
        clones.forEach { it.tick(time, delta) }
    }

    init {
        val base = context.modelD("SpaceLaserBase.obj", "Wood.bmp")
        (base as Node).apply {
            this[1].material = context.assetLoader.reflectiveMaterial("Assets/ShinySilver.bmp")
            this[2].material = context.assetLoader.diffuseMaterial("Assets/RubberFoot.bmp")
        }

        with(geometry) {
            +base
            loc = v3(0, 10, -30)
        }

        clones.forEach {
            it as SpaceLaserClone
            val glowMaterial = context.assetLoader.diffuseMaterial("Assets/" + type.filename).apply {
                setColor("GlowColor", type.glowColor)
            }
            with(it.shooter as Node) {
                this[0].material = context.assetLoader.reflectiveMaterial("Assets/HornSkinGrey.bmp")
                this[1].material = context.assetLoader.diffuseMaterial("Assets/RubberFoot.bmp")
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
                setUserData("bounding_box", false)
            }
        }
        private var wobbleIntensity = 0.0
        private val angleCalculator = SigmoidAngleCalculator
        internal val shooter: Spatial = with(laserNode) {
            +context.modelD("SpaceLaser.obj", "ShinySilver.bmp")
        }
        private val index: NumberSmoother = NumberSmoother(0f, 17.0)

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)

            currentNotePeriod?.run {
                adjustPlayingRotation(this, delta)
                scaleByStageCollision()
            } ?: run { adjustIdleRotation(time, delta) }

            laserNode.rot = v3(0, 0, rotation + sin(50 * wobbleTime) * wobbleIntensity)

            // Only show laser beam if currently playing
            laserBeam.cullHint = isPlaying.ch
        }

        private fun adjustPlayingRotation(np: TimedArc, delta: Duration) {
            // Currently playing, so set the correct rotation
            rotation = angleCalculator.angleFromNote(np.note, pitchBendAmount)

            // If just starting playing, reset modulation so that we start with no vibrato
            if (wobbleTime == 0.0) {
                pitchBendModulationController.resetModulation()
            }
            wobbleTime += delta.toDouble(SECONDS)

            // Start wobbling 0.1 secs after starting playing
            wobbleIntensity = (wobbleTime - 0.1).coerceIn(0.0..0.07)
        }

        private fun adjustIdleRotation(time: Duration, delta: Duration) {
            // Not yet playing. Look ahead to the next NotePeriod
            timedArcCollector.peek()?.let {
                val startTime = it.startTime
                if (startTime - time <= 1.seconds) { // Less than 1 second away from playing
                    val targetPos = angleCalculator.angleFromNote(
                        it.note,
                        pitchBendModulationController.getPitchBendAtTick(it.start).toFloat()
                    )
                    if (startTime - time >= delta) {
                        // Slowly inch our way to the target rotation
                        rotation += (targetPos - rotation) / (startTime - time).toDouble(SECONDS) * delta.toDouble(
                            SECONDS
                        )
                    }
                }
            }
            wobbleTime = 0.0
        }

        private fun scaleByStageCollision() = CollisionResults().apply {
            context.stageManager.stageNode.collideWith(
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

        override fun adjustForPolyphony(delta: Duration) {
            laserNode.loc = v3(0, 0, index.tick(delta) { indexForMoving() * 5f } + 6)
        }

        override fun createCollector() {
            timedArcCollector = TimedArcCollector(context, arcs) { time: Duration, arc: TimedArc ->
                time >= arc.endTime - earlyReleases[arc]!!
            }
        }

        override fun toString(): String {
            return super.toString() + buildString {
                append(debugProperty("rotation", rotation.toFloat()))
                append(debugProperty("wobble", wobbleIntensity.toFloat()))
                append(debugProperty("pitchBendAmount", pitchBendAmount))
            }
        }
    }
}
