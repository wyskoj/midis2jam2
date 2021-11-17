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

package org.wysko.midis2jam2.midi

import org.wysko.midis2jam2.starter.SequencerHandler
import java.util.*
import javax.sound.midi.Sequencer

/** An implementation of SequencerHandler that uses Java's Sequencer. */
class JavaXSequencer(
    /** The Sequencer to use. */
    val sequencer: Sequencer
) : SequencerHandler {

    override fun isOpen(): Boolean = sequencer.isOpen

    override fun start(midiFile: MidiFile) {
        sequencer.tempoInBPM = midiFile.firstTempoInBpm().toFloat()
        sequencer.start()
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                /* Find the first tempo we haven't hit and need to execute */
                val currentMidiTick = sequencer.tickPosition
                for (tempo in midiFile.tempos) {
                    if (tempo.time == currentMidiTick) {
                        sequencer.tempoInBPM = 60000000f / tempo.number
                    }
                }
            }
        }, 0, 1)
    }

    override fun stop() {
        if (sequencer.isRunning)
            sequencer.stop()

        if (sequencer.isOpen)
            sequencer.close()
    }

    override fun position(): Long = sequencer.microsecondPosition

    override fun duration(): Long = sequencer.microsecondLength
}