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

import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.renderer.queue.RenderQueue.ShadowMode.Off
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.instrument.RisingPitchClassAnimator
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import kotlin.math.exp
import kotlin.math.pow
import kotlin.time.Duration

private val BASE_POSITION = Vector3f(0f, 29.5f, -152.65f)

/**
 * The Stage Choir.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 * @param type The type of choir peep.
 */
class StageChoir(context: Midis2jam2, eventList: List<MidiEvent>, type: ChoirType) :
    DivisiveSustainedInstrument(context, eventList) {

    private val pitchBendModulationController = PitchBendModulationController(context, eventList)
    private var bend = 0f

    override val animators: List<PitchClassAnimator> = List(12) {
        val notePeriods = eventList.notePeriodsModulus(context, it)
        when (type) {
            ChoirType.HaloSynth -> ChoirPeepHalo(notePeriods)
            else -> ChoirPeep(type, notePeriods)
        }
    }

    init {
        repeat(12) {
            with(geometry) {
                +node {
                    +animators[it].root
                    rot = v3(0, 11.27 + it * -5.636, 0)
                }
            }
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        bend = pitchBendModulationController.tick(time, delta)
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        val indexForMoving = updateInstrumentIndex(delta)
        animators.forEach {
            it as ChoirPeep
            if (indexForMoving >= 0) {
                it.root.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, 10f, -15f).mult(indexForMoving))
            } else {
                it.root.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, indexForMoving * 10f, indexForMoving * 10f))
            }
        }
    }

    override fun findSimilar(): List<Instrument> =
        context.instruments.filterIsInstance<StageChoir>() + context.instruments.filterIsInstance<ApplauseChoir>()

    /** A single choir peep. */
    open inner class ChoirPeep(type: ChoirType, notePeriods: List<TimedArc>) :
        RisingPitchClassAnimator(context, notePeriods) {
        private val head = with(geometry) {
            +context.modelD("StageChoirHead.obj", type.textureFile).also { it.move(v3(0, 24.652, 0)) }
        }

        init {
            with(geometry) {
                +context.modelD("StageChoirBody.obj", type.textureFile)
            }
            root.loc = BASE_POSITION
        }

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)
            head.rot = v3(if (playing) (1.0 / (1 + exp(bend / 4))) * 180 - 90 else 0, 0, 0)
        }
    }

    /** A single choir peep with a halo. */
    inner class ChoirPeepHalo(notePeriods: List<TimedArc>) : ChoirPeep(ChoirType.HaloSynth, notePeriods) {
        private val halo = with(geometry) {
            +context.modelD("StageChoirHalo.obj", "ChoirHalo.png").also {
                it.material = context.diffuseMaterial("ChoirHalo.png")
                it.shadowMode = Off
            }
        }

        override fun tick(time: Duration, delta: Duration) {
            super.tick(time, delta)
            val progress = collector.currentTimedArcs.firstOrNull()?.calculateProgress(time) ?: 1.0
            val glowIntensity = (-progress.pow(64) + 1).toFloat()
            halo.material.setColor("GlowColor", ColorRGBA(glowIntensity, glowIntensity, 0f, 1f))
        }
    }

    /**
     * The type of choir peep.
     *
     * @property textureFile The texture file for the choir peep.
     */
    enum class ChoirType(val textureFile: String) {
        /** Voice aahs. */
        ChoirAahs("ChoirPeep.bmp"),

        /** Voice oohs. */
        VoiceOohs("ChoirPeepOoh.png"),

        /** Synth voice. */
        SynthVoice("ChoirPeepSynthVoice.png"),

        /** Voice synth. */
        VoiceSynth("ChoirPeepVoiceSynth.png"),

        /** Halo synth. */
        HaloSynth("ChoirHalo.png"),

        /** Goblin synth. */
        GoblinSynth("ChoirPeepGoblin.png"),
    }
}
