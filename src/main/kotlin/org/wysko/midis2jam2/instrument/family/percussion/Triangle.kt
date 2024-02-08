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

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR

/** The Triangle. */
class Triangle(
    context: Midis2jam2,
    muteHits: MutableList<MidiNoteOnEvent>,
    openHits: MutableList<MidiNoteOnEvent>,
) : AuxiliaryPercussion(context, (muteHits + openHits).sortedBy { it.time }.toMutableList()) {
    private val muteCollector = EventCollector(muteHits, context)
    private val openCollector = EventCollector(openHits, context)

    private val fist = with(recoilNode) {
        +context.modelD("MutedTriangle.obj", "hands.bmp").apply {
            cullHint = false.ch
        }
    }

    private val beater =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = context.modelR("Triangle_Stick.obj", "ShinySilver.bmp"),
        ).apply {
            setParent(geometry)
            node.move(0f, 2f, 4f)
        }


    init {
        with(recoilNode) {
            +context.modelR("Triangle.obj", "ShinySilver.bmp")
            rot = v3(0, 0, 45)
        }

        geometry.setLocalTranslation(0f, 53f, -57f)

        /* By rotating the recoil node on a 45, the direction of recoil is SW, then the whole thing is rotated back
         * so that it appears upright. */
        geometry.rot = v3(0f, 0f, -45f)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        // Beater
        val results = beater.tick(time, delta)
        recoilDrum(recoilNode, results.velocity, delta)

        // Fist
        muteCollector.advanceCollectOne(time)?.let {
            fist.cullHint = true.ch
        }
        openCollector.advanceCollectOne(time)?.let {
            fist.cullHint = false.ch
        }
    }

    override fun onEntry() {
        val nextMute = muteCollector.peek()
        val nextOpen = openCollector.peek()

        fist.cullHint = when {
            nextMute == null && nextOpen != null -> {
                false
            }

            nextMute != null && nextOpen == null -> {
                true
            }

            nextMute != null && nextOpen != null -> {
                nextMute.time < nextOpen.time
            }

            else -> error("All cases covered.")
        }.ch
    }
}
