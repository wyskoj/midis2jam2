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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.kit

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType.DRUM_SET_STICK
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSetInstrument
import org.wysko.midis2jam2.midi.ACOUSTIC_SNARE
import org.wysko.midis2jam2.midi.ELECTRIC_SNARE
import org.wysko.midis2jam2.midi.SIDE_STICK
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.max
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/** The Snare drum. */
class SnareDrum(context: Midis2jam2, hits: MutableList<NoteEvent.NoteOn>, style: ShellStyle) :
    DrumSetInstrument(context, hits) {
    private val regularStick =
        Striker(
            context = context,
            strikeEvents = hits.filter { it.note == ACOUSTIC_SNARE || it.note == ELECTRIC_SNARE },
            stickModel = DRUM_SET_STICK,
        ).apply {
            setParent(recoilNode)
        }.also {
            it.node.move(10f, 0f, 3f)
            it.node.rotate(0f, rad(80.0), 0f)
        }

    private val sideStick =
        Striker(
            context = context,
            strikeEvents = hits.filter { it.note == SIDE_STICK },
            stickModel = DRUM_SET_STICK,
        ).apply {
            setParent(recoilNode)
            offsetStick {
                it.move(0f, 0f, -2f)
                it.rotate(0f, rad(-20.0), 0f)
            }
        }.also {
            it.node.apply {
                setLocalTranslation(-1f, 0.4f, 6f)
            }
        }

    init {
        context.modelD(style.snareDrumModel, style.snareShellTexture).apply {
            recoilNode.attachChild(this)
            if (style is ShellStyle.AlternativeDrumShell) scale(1.2f) // Looks more aesthetically pleasing
        }

        geometry.apply {
            move(-10.9f, 16f, -72.5f)
            rotate(rad(10.0), 0f, rad(-10.0))
        }
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        val regularResults = regularStick.tick(time, delta)
        val sideResults = sideStick.tick(time, delta)

        recoilDrum(
            drum = recoilNode,
            velocity = max(regularResults.strike?.velocity ?: 0, sideResults.strike?.velocity ?: 0),
            delta = delta,
        )
    }
}
