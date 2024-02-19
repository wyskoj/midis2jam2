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
package org.wysko.midis2jam2.instrument

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.FingeringManager
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.instrument.clone.debugString
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.times
import kotlin.math.PI
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * An instrument that uses [Clone]s to represent polyphony.
 *
 * It just happens to be that all monophonic instruments in the MIDI standard are also [SustainedInstrument]s.
 *
 * @param context The context to the main class.
 * @param eventList The list of all events that this instrument should be aware of.
 * @param cloneClass The class that this instrument uses to represent polyphony.
 * @property manager The [FingeringManager].
 * @see Clone
 */
abstract class MonophonicInstrument protected constructor(
    context: Midis2jam2,
    eventList: List<MidiChannelEvent>,
    cloneClass: KClass<out Clone>,
    val manager: FingeringManager<*>?,
) : SustainedInstrument(context, eventList) {

    /**
     * The controller that manages pitch bend. Many instruments have pitch bend, so it is abstracted to this class.
     */
    protected open val pitchBendModulationController: PitchBendModulationController =
        PitchBendModulationController(context, eventList, smoothness = 10.0)

    /**
     * The configuration of how pitch bend should be applied to the clones.
     */
    protected open val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration()

    /**
     * [notePeriods] assigned to [Clone]s.
     */
    val clones: List<Clone> = run {
        val sortedNotePeriods = notePeriods.sortedWith(compareBy({ it.startTick }, { it.note }))

        val bins = sortedNotePeriods.fold(mutableListOf<MutableList<NotePeriod>>()) { list, notePeriod ->
            // Search for a clone that isn't playing (or is just about to finish playing, helps with small overlaps)
            val firstAvailableClone =
                list.firstOrNull { it.last().endTick - (context.file.division / 8) <= notePeriod.startTick }

            if (firstAvailableClone == null) {
                list += mutableListOf(notePeriod)
            } else {
                firstAvailableClone += notePeriod
            }

            list
        }

        bins.map {
            cloneClass.primaryConstructor!!.call(this).apply {
                notePeriods.addAll(it)
                createCollector()
            }
        }
    }

    /**
     * Most monophonic instruments animate pitch bend in the same way (a rotation along an axis), so we implement it
     * here.
     *
     * @param time The current time since the beginning of the song, in seconds.
     * @param delta The amount of time that elapsed since the last frame, in seconds.
     */
    open fun handlePitchBend(time: Double, delta: Float) {
        val bend = pitchBendModulationController.tick(time, delta) { collector.currentNotePeriods.isNotEmpty() }
        val rotation = (if (pitchBendConfiguration.reversed) -bend else bend) * pitchBendConfiguration.scaleFactor

        clones.forEach {
            it.bendNode.rot = pitchBendConfiguration.rotationalAxis.identity.times(rotation * (180 / PI))
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        clones.forEach { it.tick(time, delta) }
        handlePitchBend(time, delta)
    }

    override fun toString(): String = super.toString() + clones.debugString()
}
