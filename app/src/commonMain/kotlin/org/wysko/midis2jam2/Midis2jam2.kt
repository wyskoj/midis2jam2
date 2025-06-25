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

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.input.controls.ActionListener
import com.jme3.material.Material
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.FadeFilter
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.Collector
import org.wysko.midis2jam2.instrument.algorithmic.InstrumentAssignment
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSetVisibilityManager
import org.wysko.midis2jam2.starter.ProgressListener
import org.wysko.midis2jam2.starter.configuration.AppSettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.util.minusAssign
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.resourceToString
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.world.AssetLoader
import org.wysko.midis2jam2.world.DebugTextController
import org.wysko.midis2jam2.world.FakeShadowsController
import org.wysko.midis2jam2.world.HudController
import org.wysko.midis2jam2.world.StandController
import org.wysko.midis2jam2.world.camera.AutoCamController
import org.wysko.midis2jam2.world.camera.CameraAngle
import org.wysko.midis2jam2.world.camera.CameraSpeed
import org.wysko.midis2jam2.world.camera.CameraState
import org.wysko.midis2jam2.world.camera.SlideCameraController
import org.wysko.midis2jam2.world.camera.SmoothFlyByCamera
import org.wysko.midis2jam2.world.lyric.LyricController
import org.wysko.midis2jam2.world.modelD
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Controls all aspects of the program. This is the main class of the program.
 *
 * @property sequence The MIDI file to be played.
 * @property fileName The name of the MIDI file.
 * @property configs The configurations to be used.
 */
abstract class Midis2jam2(
    val sequence: TimeBasedSequence,
    open val fileName: String,
    val configs: Collection<Configuration>,
) : AbstractAppState(),
    ActionListener {
    private val _progressListeners: MutableList<ProgressListener> = mutableListOf()

    fun registerProgressListener(listener: ProgressListener) {
        _progressListeners.add(listener)
    }

    /**
     * The root node of the scene.
     */
    val root: Node = Node()

    /**
     * The AssetManager, as provided by the [app].
     */
    val assetManager: AssetManager by lazy { app.assetManager }

    /**
     * The version of midis2jam2.
     */
    val version: String = resourceToString("/version.txt")

    /**
     * The build information of midis2jam2.
     */
    val build: String = resourceToString("/build.txt")

    /**
     * If this value is positive, this is the amount of time that has elapsed since the beginning of the MIDI sequence.
     * If this value is negative, the MIDI sequence has not started yet, and is the negation of the time until the
     * sequence starts.
     */
    var time: Duration = (-2.0).seconds

    /** The current state of the camera. */
    var cameraState: CameraState =
        if (configs.find<AppSettingsConfiguration>().appSettings.cameraSettings.isStartAutocamWithSong) {
            CameraState.AUTO_CAM
        } else {
            CameraState.FREE_CAM
        }
        set(value) {
            // Enable/disable controllers based on camera state when value is changed
            autocamController.enabled = value == CameraState.AUTO_CAM
            flyByCamera.isEnabled = value == CameraState.FREE_CAM
            slideCamController.isEnabled = value == CameraState.SLIDE_CAM
            field = value
        }

    /**
     * The jMonkeyEngine application.
     */
    lateinit var app: SimpleApplication

    /**
     * The AssetLoader for this instance.
     */
    lateinit var assetLoader: AssetLoader

    /**
     * The stage of the scene.
     */
    lateinit var stage: Spatial

    /**
     * The drum set visibility manager.
     */
    lateinit var drumSetVisibilityManager: DrumSetVisibilityManager

    /**
     * The list of collectors.
     */
    protected val collectors: MutableList<Collector<*>> = mutableListOf()

    /**
     * `true` if the sequencer has reached the end of the MIDI file, `false` otherwise.
     */
    protected var isSongFinished: Boolean = false

    /**
     * `true` if the sequence is paused, `false` otherwise.
     */
    protected var paused: Boolean = false

    /**
     * When the MIDI sequence ends, the [time] is recorded to this variable to know when to close the
     * app.
     */
    protected var endTime: Duration by Delegates.notNull()

    /**
     * Used for the fade in of the performance screen.
     */
    protected lateinit var fadeFilter: FadeFilter

    /**
     * The shadow controller.
     */
    var fakeShadowsController: FakeShadowsController? = null

    /**
     * The lyric controller.
     */
    var lyricController: LyricController? = null

    /**
     * The list of instruments.
     */
    lateinit var instruments: List<Instrument>

    /**
     * The stand controller.
     */
    lateinit var standController: StandController

    /**
     * The autocam controller.
     */
    lateinit var autocamController: AutoCamController

    /**
     * The slide-cam controller.
     */
    lateinit var slideCamController: SlideCameraController

    /**
     * The HUD controller.
     */
    lateinit var hudController: HudController

    /**
     * The fly-by camera.
     */
    lateinit var flyByCamera: SmoothFlyByCamera

    protected lateinit var debugTextController: DebugTextController
    private var currentCameraAngle: CameraAngle = CameraAngle.CAMERA_1A
        set(value) {
            cameraState = CameraState.FREE_CAM
            flyByCamera.setTargetTransform(value.location, value.rotation)
            field = value
        }

    var cameraSpeed: CameraSpeed = CameraSpeed.Normal

    override fun initialize(stateManager: AppStateManager, app: Application) {
        val settingsConfig = configs.find<AppSettingsConfiguration>().appSettings
        this.app = app as SimpleApplication
        this.assetLoader = AssetLoader(this) { assetName ->
            _progressListeners.onEach { it.onLoadingAsset(assetName) }
        }
        this.flyByCamera =
            SmoothFlyByCamera(this) {
                isEnabled = true
                cameraState = CameraState.FREE_CAM
            }.apply {
                actLikeNormalFlyByCamera = !settingsConfig.cameraSettings.isSmoothFreecam
                isEnabled = !settingsConfig.cameraSettings.isStartAutocamWithSong
            }
        this.stage = with(root) { +modelD("Stage.obj", "Stage.bmp") }.also {
            it.shadowMode = RenderQueue.ShadowMode.Receive
        }
        this.fadeFilter = FadeFilter(0.5f).apply { value = 0f }.also {
            filterPostProcessor()?.addFilter(it)
        }
        this.instruments =
            InstrumentAssignment.assign(this, midiFile = sequence, onLoadingProgress = { progress ->
                _progressListeners.onEach { it.onLoadingProgress(progress) }
            }).onEach {
                // This is a bit of a hack to prevent stuttering when the instrument would first appear
                root += it.root
                root -= it.root
            }
        this.drumSetVisibilityManager = DrumSetVisibilityManager(this, instruments.filterIsInstance<DrumSet>())
        this.standController = StandController(this)
        this.lyricController =
            if (settingsConfig.onScreenElementsSettings.lyricsSettings.isShowLyrics) {
                LyricController(
                    this,
                    sequence.smf.tracks
                        .flatMap { it.events }
                        .filterIsInstance<MetaEvent.Lyric>(),
                )
            } else {
                null
            }
        this.autocamController = AutoCamController(this, settingsConfig.cameraSettings.isStartAutocamWithSong)
        this.slideCamController = SlideCameraController(this)
        this.debugTextController = DebugTextController(this)
        this.hudController = HudController(this)
        if (settingsConfig.cameraSettings.isStartAutocamWithSong) {
            with(this.app.camera) {
                let {
                    location = it.location
                    rotation = it.rotation
                }
            }
        }
        if (!configs.find<AppSettingsConfiguration>().appSettings.graphicsSettings.shadowsSettings.isUseShadows) {
            fakeShadowsController = FakeShadowsController(this)
        }

        handleResetMessage()


        super.initialize(stateManager, app)
        _progressListeners.onEach { it.onReady() }
    }

    private fun filterPostProcessor() =
        this.app.viewPort.processors.filterIsInstance<FilterPostProcessor>().firstOrNull()

    override fun update(tpf: Float) {
        super.update(tpf)
        debugTextController.tick(tpf.toDouble().seconds)
    }

    /** Sets the speed of the camera, given a speed [name] and whether that key is [pressed]. */
    private fun handleCameraSpeedPress(name: String, pressed: Boolean) {
        if (CameraSpeed.entries.none { it.name.lowercase() == name }) return

        val pressedCameraSpeed = CameraSpeed[name]
        when (configs.find<AppSettingsConfiguration>().appSettings.controlsSettings.isSpeedModifierKeysSticky) {
            true -> {
                if (pressed) return // Only on release
                cameraSpeed = if (pressedCameraSpeed == cameraSpeed) CameraSpeed.Normal else pressedCameraSpeed
            }

            false -> {
                cameraSpeed = if (pressed) pressedCameraSpeed else CameraSpeed.Normal
            }
        }
        applyCameraSpeed(cameraSpeed)
    }

    private fun applyCameraSpeed(speed: CameraSpeed) {
        flyByCamera.moveSpeed = speed.speedValue
    }

    /**
     * Handles when a key is pressed, setting the correct camera position.
     *
     * @param name The name of the key-binding pressed.
     * @param isPressed `true` if the key is pressed, `false` otherwise.
     */
    private fun handleCameraSetting(
        name: String,
        isPressed: Boolean,
    ) {
        if (!isPressed) return
        when (name) {
            "autoCam" -> {
                autocamController.trigger()
                cameraState = CameraState.AUTO_CAM
            }

            "slideCam" -> {
                cameraState = CameraState.SLIDE_CAM
            }
        }
        if (name.startsWith("cam")) {
            currentCameraAngle = CameraAngle.handleCameraAngle(currentCameraAngle, name)
            cameraState = CameraState.FREE_CAM
        }
    }

    protected fun handleResetMessage() {
        val config =
            configs.find<AppSettingsConfiguration>().appSettings.playbackSettings.midiSpecificationResetSettings
        if (config.isSendSpecificationResetMessage) {
            sendResetMessage(config.midiSpecification)
            logger().debug("Sent ${config.midiSpecification} reset message to MIDI device")
        }
    }

    abstract fun sendResetMessage(midiSpecification: MidiSpecification)

    /**
     * Called when an input to which this listener is registered to is invoked.
     *
     * @param name The name of the mapping that was invoked.
     * @param isPressed `true` if the action is "pressed", `false` otherwise
     * @param tpf The amount of time that has passed since the last frame, in seconds.
     */
    override fun onAction(
        name: String,
        isPressed: Boolean,
        tpf: Float,
    ) {
        handleCameraSpeedPress(name, isPressed)
        handleCameraSetting(name, isPressed)
        if (isPressed) {
            when (name) {
                "exit" -> exit()
                "debug" -> debugTextController.toggle()
                "seek_forward" -> seek(time + 10.seconds)
                "seek_backward" -> seek((time - 10.seconds).coerceAtLeast(0.seconds))
                "play/pause" -> togglePause()
            }
        }
    }

    /** Seeks to a given point in time. */
    abstract fun seek(time: Duration)

    /** Exits the application. */
    abstract fun exit()

    /** Pauses or resumes the application, depending on the current [paused] state. */
    protected open fun togglePause() {
        paused = !paused
    }

    /** Loads and returns a reflective material with the specified [texture] at a default brightness. */
    fun reflectiveMaterial(texture: String): Material = assetLoader.reflectiveMaterial(texture)

    /** Loads and returns an unshaded material with the specified [texture]. */
    fun diffuseMaterial(texture: String): Material = assetLoader.diffuseMaterial(texture)

    /**
     * Registers a [collector] to be updated when the time is advanced.
     *
     * @param collector The collector to be registered.
     */
    fun registerCollector(collector: Collector<*>) {
        collectors += collector
    }

    override fun stateDetached(stateManager: AppStateManager?) {
        super.stateDetached(stateManager)
        filterPostProcessor()?.removeFilter(fadeFilter)
    }

    protected fun unloadFlyByCamera() {
        flyByCamera.isEnabled = false
        app.flyByCamera.isEnabled = false
    }
}
