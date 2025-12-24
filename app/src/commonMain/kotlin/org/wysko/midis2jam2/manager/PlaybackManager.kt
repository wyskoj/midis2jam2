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

package org.wysko.midis2jam2.manager

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.input.controls.ActionListener
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_PLAY
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_RESTART
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_SEEK_BACKWARD
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_SEEK_FORWARD
import org.wysko.midis2jam2.manager.CollectorsManager.Companion.collectorsManager
import org.wysko.midis2jam2.manager.PlaybackManager.SeekDirection.Backward
import org.wysko.midis2jam2.manager.PlaybackManager.SeekDirection.Forward
import org.wysko.midis2jam2.midi.system.JwSequencer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

private val INTRO = 2.0.seconds
private val OUTRO = 3.0.seconds

class PlaybackManager(
    private val sequence: TimeBasedSequence,
    private val sequencer: JwSequencer,
    private val isLooping: Boolean,
) : BaseManager(), ActionListener {
    val duration: Duration = sequence.duration
    var time: Duration = -INTRO
    var isPlaying: Boolean = true

    private var isSequencerStarted: Boolean = false
    private var isWaitingForSequencerSeek = false
    private var skippedFrames = 0

    override fun initialize(app: Application) {
        super.initialize(app)
        app.inputManager.addListener(
            this, ACTION_PLAY, ACTION_SEEK_FORWARD, ACTION_SEEK_BACKWARD, ACTION_RESTART
        )
    }

    override fun update(tpf: Float) {
        if (skippedFrames++ < 3) return

        if (!isSequencerStarted && time > ZERO && isPlaying) {
            sequencer.start()
            isSequencerStarted = true
        }

        if (isPlaying && !isWaitingForSequencerSeek) {
            time += tpf.toDouble().seconds
        }

        if (time > duration + OUTRO) {
            when (isLooping) {
                true -> onLoop()
                false -> app.stop()
            }
        }
    }

    private fun onLoop() {
        isSequencerStarted = false
        time = -INTRO
        sequencer.setPosition(ZERO, false, ::onSequencerSetPositionFinished)
        isWaitingForSequencerSeek = true
        app.collectorsManager.seek(-INTRO)
        midiDeviceManager.sendResetMessage()
    }

    fun seek(time: Duration) {
        this.time = time
        when {
            time > duration -> sequencer.stop()
            else -> {
                sequencer.setPosition(time, isPlaying, ::onSequencerSetPositionFinished)
                isWaitingForSequencerSeek = true
            }
        }
        app.collectorsManager.seek(time)
    }

    override fun cleanup(app: Application?) {
        sequencer.run {
            stop()
            close()
        }
    }

    override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
        if (!isPressed) return

        when (name) {
            ACTION_PLAY -> togglePlayPause()
            ACTION_SEEK_FORWARD -> seek(Forward)
            ACTION_SEEK_BACKWARD -> seek(Backward)
            ACTION_RESTART -> seek(ZERO)
        }
    }

    fun togglePlayPause() {
        setIsPlaying(!isPlaying)
    }

    private fun setIsPlaying(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        when (isPlaying) {
            true -> sequencer.start()
            false -> sequencer.stop()
        }
    }

    fun seek(direction: SeekDirection) {
        when (direction) {
            Forward -> seek(time + 10.seconds)
            Backward -> seek((time - 10.seconds).coerceAtLeast(ZERO))
        }
    }

    private val midiDeviceManager
        get() = stateManager.getState(MidiDeviceManager::class.java)

    private fun onSequencerSetPositionFinished() {
        isWaitingForSequencerSeek = false
    }

    companion object {
        val SimpleApplication.time: Duration
            get() = stateManager.getState(PlaybackManager::class.java).time
        val SimpleApplication.sequence: TimeBasedSequence
            get() = stateManager.getState(PlaybackManager::class.java).sequence
    }

    enum class SeekDirection {
        Forward, Backward
    }
}