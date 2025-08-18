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

package org.wysko.midis2jam2

import Platform
import com.jme3.app.Application
import com.jme3.app.state.AppStateManager
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.domain.FluidSynthBridge
import org.wysko.midis2jam2.domain.FluidSynthDevice
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.midi.sendResetMessage
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.starter.configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.HomeConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.background.BackgroundController
import org.wysko.midis2jam2.world.camera.CameraAngle
import org.wysko.midis2jam2.world.camera.CameraAngle.Companion.preventCameraFromLeaving
import org.wysko.midis2jam2.world.camera.CameraState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class AndroidMidis2jam2(
    override val fileName: String,
    val midiFile: TimeBasedSequence,
    val onClose: () -> Unit,
    val sequencer: JwSequencer,
    val midiDevice: MidiDevice,
    configs: Collection<Configuration>,
) : Midis2jam2(midiFile, fileName, configs) {
    private var isSequencerStarted: Boolean = false
    private var skippedFrames = 0

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        BackgroundController.configureBackground(this@AndroidMidis2jam2, configs.find(), root, Platform.Android)
        cameraController = AndroidOrbitingCamera(this)
        setCameraControllersActive(cameraState)

        if (configs.find<AppSettingsConfiguration>().appSettings.generalSettings.isShowDebugInfo) {
            debugTextController.toggle()
        }
    }

    override fun sendResetMessage(midiSpecification: MidiSpecification) {
        midiDevice.sendResetMessage(midiSpecification)
    }

    override fun cleanup() {
        logger().debug("Cleaning up...")
        sequencer.run {
            stop()
            close()
        }
        onClose()
        logger().debug("Cleanup complete")
    }

    override fun update(tpf: Float) {
        super.update(tpf)

        val delta = tpf.toDouble().seconds

        startSequencerIfNeeded()

        fadeFilter.value = when {
            time in (-2.0).seconds..(-1.5).seconds -> {
                0.0f
            }

            time in (-1.5).seconds..(-1.0).seconds -> {
                Utils.mapRangeClamped(time.toDouble(DurationUnit.SECONDS), -1.5, -1.0, 0.0, 1.0)
                    .toFloat()
            }

            (midiFile.duration + 3.seconds - time) < 0.5.seconds -> {
                Utils.mapRangeClamped(
                    time.toDouble(DurationUnit.SECONDS),
                    ((midiFile.duration + 3.seconds) - 0.5.seconds).toDouble(DurationUnit.SECONDS),
                    (midiFile.duration + 3.seconds).toDouble(DurationUnit.SECONDS),
                    1.0,
                    0.0
                ).toFloat()
            }

            else -> 1.0f
        }

        instruments.forEach { it.tick(time, delta) }

        if (time >= sequence.duration) {
            if (!isSongFinished) endTime = time
            isSongFinished = true
        }

        handleSongCompletion()

        // This is a hack to prevent the first few frames from updating the timeSinceStart variable.
        if (skippedFrames++ < 3) {
            return
        }

        tickControllers(delta)

        if (sequencer.isOpen && !paused) {
            time += delta
        }
    }

    override fun exit() {
        logger().debug("Exiting...")
        if (sequencer.isOpen) {
            sequencer.stop()
        }
        app.run {
            stateManager.detach(this@AndroidMidis2jam2)
            stop(false)
        }
        onClose()
        logger().debug("Exit complete")
    }

    override fun seek(time: Duration) {
        logger().debug("Seeking to time: $time")
        this.time = time
        sequencer.setPosition(time.coerceAtLeast(ZERO))
        collectors.forEach { it.seek(time) }
    }

    override fun togglePause() {
        super.togglePause()
        if (!isSequencerStarted) return

        if (paused) {
            sequencer.stop()
        } else {
            sequencer.start()
        }
    }

    /**
     * Called to handle the completion of the song.
     */
    private fun handleSongCompletion() {
        if (isSongFinished && time >= endTime + 3.seconds) {
            if (configs.find<HomeConfiguration>().isLooping) {
                // Loop the song
                isSequencerStarted = false
                sequencer.stop()
                seek((-2).seconds)
                slideCamController.onLoop()
                sequencer.resetDevice()
            } else {
                exit()
            }
        }
    }

    private fun tickControllers(delta: Duration) {
        fakeShadowsController?.tick()
        standController.tick()
        lyricController?.tick(time, delta)
        hudController.tick(time, fadeFilter.value)
        autocamController.tick(time, delta)
        slideCamController.tick(time, delta)
        preventCameraFromLeaving(app.camera)
        drumSetVisibilityManager.tick(time)
    }

    private fun startSequencerIfNeeded() {
        if (!isSequencerStarted && time > ZERO) {
            sequencer.start()
            isSequencerStarted = true
            setSynthEffects()
        }
    }

    private fun setSynthEffects() {
        val config =
            configs.find<AppSettingsConfiguration>().appSettings.playbackSettings.synthesizerSettings
        if (!config.isUseChorus) {
            FluidSynthDevice.device.setChorusActive(false)
        }
        if (!config.isUseReverb) {
            FluidSynthDevice.device.setReverbActive(false)
        }
    }

    fun callAction(action: Midis2jam2Action) {
        val isDisableTouchInput =
            configs.find<AppSettingsConfiguration>().appSettings.controlsSettings.isDisableTouchInput
        when (action) {
            is Midis2jam2Action.MoveToCameraAngle -> {
                currentCameraAngle =
                    CameraAngle.handleCameraAngle(currentCameraAngle, action.cameraAngle)
                cameraState = CameraState.DEVICE_SPECIFIC_CAMERA
            }

            Midis2jam2Action.SwitchToAutoCam -> {
                onAction("autoCam", true, 0f)
            }

            Midis2jam2Action.SwitchToSlideCam -> {
                onAction("slideCam", true, 0f)
            }

            Midis2jam2Action.PlayPause -> onAction("play/pause", true, 0f)
            Midis2jam2Action.SeekBackward -> onAction("seek_backward", true, 0f)
            Midis2jam2Action.SeekForward -> onAction("seek_forward", true, 0f)
            is Midis2jam2Action.Zoom -> {
                if (isDisableTouchInput) return
                if (cameraState != CameraState.DEVICE_SPECIFIC_CAMERA) {
                    (cameraController as AndroidOrbitingCamera).applyFakeOrigin()
                }
                cameraState = CameraState.DEVICE_SPECIFIC_CAMERA
                (cameraController as AndroidOrbitingCamera).zoom(action.zoomDelta)
            }

            is Midis2jam2Action.Pan -> {
                if (isDisableTouchInput) return
                if (cameraState != CameraState.DEVICE_SPECIFIC_CAMERA) {
                    (cameraController as AndroidOrbitingCamera).applyFakeOrigin()
                }
                cameraState = CameraState.DEVICE_SPECIFIC_CAMERA
                (cameraController as AndroidOrbitingCamera).pan(action.panDeltaX, action.panDeltaY)
            }

            is Midis2jam2Action.Orbit -> {
                if (isDisableTouchInput) return
                if (cameraState != CameraState.DEVICE_SPECIFIC_CAMERA) {
                    (cameraController as AndroidOrbitingCamera).applyFakeOrigin()
                }
                cameraState = CameraState.DEVICE_SPECIFIC_CAMERA
                (cameraController as AndroidOrbitingCamera).orbit(action.x, action.y)
            }
        }
    }

    fun isPlaying(): Boolean = sequencer.isRunning && !paused
}