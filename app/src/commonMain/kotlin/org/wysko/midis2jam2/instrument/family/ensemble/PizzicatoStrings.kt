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
package org.wysko.midis2jam2.instrument.family.ensemble

import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.ColorRGBA
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.StringLight
import org.wysko.midis2jam2.instrument.algorithmic.StringRingEnvelope
import org.wysko.midis2jam2.instrument.algorithmic.StringVibrationController
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave.TwelfthOfOctaveDecayed
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.STRING_GLOW
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How quickly a plucked string dies away: it halves in strength this often. */
private val RING_HALF_LIFE = 60.milliseconds

/** How far a fresh pluck swings the string, relative to its model's furthest frame; it dies away too fast to see less. */
private const val PLUCK_REACH = 1.5f

/** How long a plucked string stays fully lit after the pluck before it starts to fade. */
private val LIGHT_HOLD = 60.milliseconds

/** How long a plucked string's light then takes to fade steadily out. */
private val LIGHT_FADE = 200.milliseconds

/** How much brighter a lit string is than one at rest: a warm white, as the stage strings' playing texture is. */
private val LIT_TINT = ColorRGBA(1.41f, 1.41f, 1.28f, 1f)

/**
 * Pizzicato strings have 12 separate strings that animate for each note. When a note is played, the string moves
 * forwards and lights up, fading after [LIGHT_HOLD] over [LIGHT_FADE], and vibrates, dying away over about 0.4 seconds.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 */
class PizzicatoStrings(
    context: PerformanceManager,
    eventList: List<MidiEvent>
) : DecayedInstrument(context, eventList) {

    private val eventCollector: EventCollector<NoteEvent.NoteOn> =
        EventCollector(context, eventList.filterIsInstance<NoteEvent.NoteOn>())

    private val strings: Array<PizzicatoString> = Array(12) {
        PizzicatoString().apply {
            with(highestLevel) {
                geometry += this
                loc = v3(2 * it, 0.5 * it, 0)
                scale = v3(1, 0.5 - 0.019 * it, 1)
            }
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        eventCollector.advanceCollectAll(time).forEach { strings[(it.note + 3) % 12].play() }
        strings.forEach { it.tick(delta) }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        root.rot = v3(0, 45 + 12 * updateInstrumentIndex(delta), 0)
    }

    init {
        placement.loc = v3(0, 6.7, -138)
    }

    /**
     * Contains a single string.
     */
    private inner class PizzicatoString : TwelfthOfOctaveDecayed() {

        private val animatedStringNode = node()
        private val restingString: Spatial = context.model(Models.Ensemble.StageStrings.String)
        private val animatedStringFrames: List<Spatial> = Models.Ensemble.StageStrings.StringBottom.map {
            context.model(it).apply {
                cullHint = Always // Hide on startup
                animatedStringNode += this
            }
        }
        private val stringAnimator: StringVibrationController =
            StringVibrationController(animatedStringFrames, reach = PLUCK_REACH)
        private val envelope = StringRingEnvelope(RING_HALF_LIFE)
        private val light = StringLight(
            material = context.assetLoader.material(Materials.Diffuse.StageString).also {
                animatedStringFrames.first().setMaterial(it)
            },
            glow = STRING_GLOW,
            tint = LIT_TINT,
            hold = LIGHT_HOLD,
            fade = LIGHT_FADE,
        )
        private var plucked = false
        private val nudgeCtrl = NumberSmoother(0f, 40.0)

        init {
            with(animNode) {
                +context.model(Models.Ensemble.PizzicatoStrings.Holder)
                +restingString
                +animatedStringNode
            }
        }

        override fun tick(delta: Duration) {
            // A pluck is held for just the frame it lands on, then rings out.
            stringAnimator.tick(delta, amplitude = envelope.tick(delta, held = plucked))
            val lit = light.tick(delta, held = plucked)
            plucked = false

            // Move string and update visibility
            val ringing = envelope.isRinging
            animatedStringNode.cullHint = ringing.ch
            restingString.cullHint = (!ringing).ch
            // The holder stays forward until the light is half gone.
            animNode.loc = v3(0, 0, nudgeCtrl.tick(delta) { if (lit > 0.5f) 2f else 0f })
        }

        fun play() {
            plucked = true
            stringAnimator.restart(fromPeak = true)
        }
    }
}
