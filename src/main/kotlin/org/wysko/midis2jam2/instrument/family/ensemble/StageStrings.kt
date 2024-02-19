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
package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.renderer.queue.RenderQueue.ShadowMode.Receive
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.MultipleInstancesRadialAdjustment
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.get
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.material
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.Axis.Y
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.math.sin

/**
 * The Stage Strings.
 */
class StageStrings(
    context: Midis2jam2,
    private val eventList: List<MidiChannelEvent>,
    type: StageStringsType,
    private val stageStringBehavior: StageStringBehavior = StageStringBehavior.Normal,
) : DivisiveSustainedInstrument(context, eventList), MultipleInstancesRadialAdjustment {

    private val stringNodes = List(12) { Node() }
    private val pitchBendModulationController = PitchBendModulationController(context, eventList, 0.0)
    private var bend = 0f

    override val animators: List<PitchClassAnimator> = List(12) { StageStringNote(type, 11 - it) }

    override val rotationAxis: Axis = Y
    override val rotationAngle: Float = 11.6f
    override val baseAngle: Float = 35.6f

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        bend = pitchBendModulationController.tick(time, delta)
    }

    init {
        repeat(12) { i ->
            stringNodes[i].run {
                +animators[i].root.apply {
                    loc = v3(0, 2 * i, -153)
                }
                rot = v3(0, 9 / 10.0 * i, 0)
            }
            geometry += stringNodes[i]
        }
    }

    /**
     * A single string.
     */
    inner class StageStringNote(type: StageStringsType, index: Int) :
        PitchClassAnimator(context, eventList.notePeriodsModulus(context, index)) {

        private val bowNode = Node()
        private val animStringNode = Node()
        private val animStrings: List<Spatial> = List(5) {
            context.modelD("StageStringBottom$it.obj", "StageStringPlaying.bmp").apply {
                cullHint = false.ch // Hide on startup
                (this as Geometry).material.setColor("GlowColor", STRING_GLOW)
            }
        }.onEach { animStringNode += it }
        private val restingString = context.modelD("StageString.obj", "StageString.bmp")
        private val bow: Spatial =
            context.modelD("StageStringBow.obj", type.textureFile).apply {
                (this as Node)[0].material = (restingString as Geometry).material
            }
        private val animator: StringVibrationController = StringVibrationController(animStrings)
        private val nudgeCtrl = NumberSmoother(-1f, if (type == StageStringsType.StringEnsemble2) 20.0 else 30.0)
        private val bendCtrl = NumberSmoother(0f, 10.0)

        init {
            with(geometry) {
                +context.modelD("StageStringHolder.obj", type.textureFile)
                +animStringNode
                +restingString
                +bowNode
            }
            with(bowNode) {
                +bow
                loc = v3(0, 48, 0)
                rot = v3(0, 0, -60)
                cullHint = false.ch
            }
            with(root) {
                shadowMode = Receive
                +geometry
            }
        }

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            bowNode.cullHint = playing.ch
            animStringNode.cullHint = playing.ch
            restingString.cullHint = (!playing).ch

            geometry.loc = v3(
                0,
                bendCtrl.tick(delta) { if (playing) bend else 0f },
                nudgeCtrl.tick(delta) { if (playing) 2f else -0.5f }
            )

            if (playing) {
                val progress = collector.currentNotePeriods.first().calculateProgress(time)
                bow.loc = when (stageStringBehavior) {
                    StageStringBehavior.Normal -> v3(0, 8 * (progress - 0.5), 0)
                    StageStringBehavior.Tremolo -> v3(0, sin(30 * time) * 4, 0)
                }
            }

            animator.tick(delta)
        }
    }

    /**
     * Defines how stage strings should behave.
     */
    enum class StageStringBehavior {
        /**
         * Normal behavior.
         *
         * The bow moves from left to right, taking the amount of time the note is held to traverse the string.
         */
        Normal,

        /**
         * Tremolo behavior.
         *
         * The bow moves back and forth for the duration of the note.
         */
        Tremolo,
    }

    /**
     * Defines how stage strings should look, depending on the MIDI patch they play.
     */
    enum class StageStringsType(internal val textureFile: String) {
        /** String Ensemble 1 type. */
        StringEnsemble1("FakeWood.bmp"),

        /** String Ensemble 2 type. */
        StringEnsemble2("Wood.bmp"),

        /** Synth Strings 1 type. */
        SynthStrings1("Laser.bmp"),

        /** Synth Strings 2 type. */
        SynthStrings2("AccordionCaseFront.bmp"),

        /** Bowed Synth type. */
        BowedSynth("SongFillbar.bmp"),
    }
}
