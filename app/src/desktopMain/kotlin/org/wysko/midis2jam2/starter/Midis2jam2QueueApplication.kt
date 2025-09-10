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

package org.wysko.midis2jam2.starter

import Platform
import com.jme3.app.SimpleApplication
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.DesktopPlaylistMidis2jam2
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.util.logger
import java.io.File
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer

internal class Midis2jam2QueueApplication(
    private val files: List<File>,
    private val configurations: Collection<Configuration>,
    private val onPlaylistFinish: () -> Unit,
    private val sequencer: Sequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
) : SimpleApplication() {
    private var currentSongIndex = 0
    private lateinit var midis2jam2s: List<DesktopPlaylistMidis2jam2>
    private var currentState: DesktopPlaylistMidis2jam2? = null

    override fun simpleInitApp() {
        setupState(configurations, platform = Platform.Desktop)
        sequencer.open()
        buildStates()
        loadSong(0)
    }

    private fun buildStates() {
        midis2jam2s = files.map { file ->
            DesktopPlaylistMidis2jam2(
                sequencer = sequencer,
                midiFile = loadKMidiSequence(file).toTimeBasedSequence(),
                onClose = {
                    stop()
                },
                onFinish = {
                    logger().debug("Advance")
                    advance()
                },
                configs = configurations,
                fileName = file.name,
                synthesizer = synthesizer,
                midiDevice = midiDevice
            )
        }
    }

    private fun loadKMidiSequence(file: File) = StandardMidiFileReader().readFile(file)
    private fun loadJavaXSequence(file: File) = MidiSystem.getSequence(file)

    private fun advance() = when {
        currentSongIndex != midis2jam2s.indices.last -> loadSong(currentSongIndex + 1)

        else -> {
            onPlaylistFinish()
            stop()
        }
    }

    private fun loadSong(index: Int) {
        rootNode.detachAllChildren()
        guiNode.detachAllChildren()
        midis2jam2s.forEach {
            stateManager.detach(it)
        }

        sequencer.stop()
        sequencer.sequence = loadJavaXSequence(files[index])

        midis2jam2s[index].let {
            stateManager.attach(it)
            rootNode.attachChild(it.root)
            currentState = it
        }

        currentSongIndex = index
    }

    override fun stop() {
        super.stop()
        onPlaylistFinish()
    }
}
