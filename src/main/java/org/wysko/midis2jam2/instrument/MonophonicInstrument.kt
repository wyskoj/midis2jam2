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
package org.wysko.midis2jam2.instrument

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.FingeringManager
import org.wysko.midis2jam2.instrument.clone.Clone
import org.wysko.midis2jam2.instrument.clone.debugString
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod

/**
 * Any instrument that can only play one note at a time (e.g., saxophones, clarinets, ocarinas, etc.).
 *
 * Because this physical limitation is lifted in MIDI files by nature, midis2jam2 needs to visualize polyphony by
 * spawning "clones" of an instrument. These clones will only appear when necessary.
 *
 * Classes that extend this will also need to specify a [Clone] class. Instantiations of this class are used to
 * represent the degree of polyphony.
 *
 * It happens to be that every monophonic instrument is also a [SustainedInstrument].
 *
 * @see Clone
 */
abstract class MonophonicInstrument protected constructor(
    /** Context to the main class. */
    context: Midis2jam2,

    /** The list of events this instrument should play. */
    eventList: List<MidiChannelSpecificEvent>,

    /** The class that this instrument uses to represent polyphony. */
    cloneClass: Class<out Clone>,

    /** The fingering manager. */
    val manager: FingeringManager<*>?
) : SustainedInstrument(context, eventList) {

    /** Node contains all clones. */
    val groupOfPolyphony: Node = Node()

    /** The list of clones this monophonic instrument needs to effectively display all notes. */
    val clones: List<Clone>

    /**
     * Since MIDI channels that play monophonic instruments can play with polyphony, we need to calculate the number of
     * "clones" needed to visualize this and determine which note events shall be assigned to which clones, using the
     * least number of clones.
     *
     * @param instrument the monophonic instrument that is handling the clones
     * @param cloneClass the class of the [Clone] to instantiate
     * @throws ReflectiveOperationException is usually thrown if an error occurs in the clone constructor
     */
    private fun calculateClones(
        instrument: MonophonicInstrument, cloneClass: Class<out Clone?>
    ): List<Clone> {
        return cloneClass.getDeclaredConstructor(instrument.javaClass).let { constructor ->
            notePeriods.sortedWith(compareBy({ it.startTick() }, { it.midiNote }))
                .fold(ArrayList<ArrayList<NotePeriod>>()) { acc, np ->
                    acc.apply {
                        acc.firstOrNull { it.last().endTick() - (context.file.division / 8) <= np.startTick() }?.add(np)
                            ?: run {
                                add(ArrayList<NotePeriod>().also { it.add(np) })
                            }
                    }
                }.map {
                    (constructor.newInstance(instrument) as Clone).apply {
                        notePeriods.addAll(it)
                    }
                }.toList()
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Tick clones */
        clones.forEach { it.tick(time, delta) }
    }

    init {
        clones = calculateClones(this, cloneClass)
        clones.forEach { groupOfPolyphony.attachChild(it.offsetNode) }
        instrumentNode.attachChild(groupOfPolyphony)
    }

    override fun toString(): String {
        return super.toString() + buildString {
            append(clones.debugString())
        }
    }
}