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

package org.wysko.midis2jam2

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.input.controls.ActionListener
import com.jme3.material.Material
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.BloomFilter
import com.jme3.post.filters.FadeFilter
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.shadow.DirectionalLightShadowFilter
import com.jme3.shadow.EdgeFilteringMode
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.InstrumentAssignment
import org.wysko.midis2jam2.instrument.algorithmic.NotePeriodCollector
import org.wysko.midis2jam2.instrument.algorithmic.NotePeriodGroupCollector
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSetVisibilityManager
import org.wysko.midis2jam2.midi.MidiEvent
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.midi.MidiTextEvent
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration.Companion.isFakeShadows
import org.wysko.midis2jam2.starter.configuration.SettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.getType
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.world.AssetLoader
import org.wysko.midis2jam2.world.DebugTextController
import org.wysko.midis2jam2.world.HudController
import org.wysko.midis2jam2.world.LightingSetup
import org.wysko.midis2jam2.world.LyricController
import org.wysko.midis2jam2.world.ShadowController
import org.wysko.midis2jam2.world.StandController
import org.wysko.midis2jam2.world.camera.AutoCamController
import org.wysko.midis2jam2.world.camera.CameraAngle
import org.wysko.midis2jam2.world.camera.CameraState
import org.wysko.midis2jam2.world.camera.SlideCameraController
import org.wysko.midis2jam2.world.camera.SmoothFlyByCamera
import org.wysko.midis2jam2.world.modelD
import kotlin.properties.Delegates

/**
 * Controls all aspects of the program. This is the main class of the program.
 *
 * @property file The MIDI file to be played.
 * @property configs The configurations to be used.
 */
abstract class Midis2jam2(
    val file: MidiFile,
    val configs: Collection<Configuration>,
) : AbstractAppState(), ActionListener {

    /**
     * The AssetManager, as provided by the [app].
     */
    val assetManager: AssetManager by lazy { app.assetManager }

    /**
     * The root node of the scene.
     */
    val rootNode: Node = Node()

    /**
     * The version of midis2jam2.
     */
    val version: String = Utils.resourceToString("/version.txt")

    /**
     * The build information of midis2jam2.
     */
    val build: String = Utils.resourceToString("/build.txt")

    /**
     * If this value is positive, this is the amount of time that has elapsed since the beginning of the MIDI sequence.
     * If this value is negative, the MIDI sequence has not started yet, and is the negation of the time until the
     * sequence starts.
     */
    var time: Double = -2.0
        protected set

    /** The current state of the camera. */
    var cameraState: CameraState = if (configs.getType(SettingsConfiguration::class).startAutocamWithSong) {
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
     * The list of event collectors.
     */
    protected val eventCollectors: MutableList<EventCollector<out MidiEvent>> = mutableListOf()

    /**
     * The list of note period collectors.
     */
    protected val notePeriodCollectors: MutableList<NotePeriodCollector> = mutableListOf()

    /**
     * The list of note period group collectors.
     */
    protected val notePeriodGroupCollectors: MutableList<NotePeriodGroupCollector> = mutableListOf()

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
    protected var endTime: Double by Delegates.notNull()

    /**
     * Used for the fade in of the performance screen.
     */
    protected lateinit var fadeFilter: FadeFilter

    /**
     * The shadow controller.
     */
    var shadowController: ShadowController? = null

    /**
     * The list of instruments.
     */
    lateinit var instruments: List<Instrument>

    /**
     * The stand controller.
     */
    lateinit var standController: StandController

    /**
     * The lyric controller.
     */
    lateinit var lyricController: LyricController

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

    private lateinit var debugTextController: DebugTextController
    private var currentCameraAngle: CameraAngle = CameraAngle.CAMERA_1A
        set(value) {
            cameraState = CameraState.FREE_CAM
            flyByCamera.setTargetTransform(value.location, value.rotation)
            field = value
        }

    override fun initialize(stateManager: AppStateManager, app: Application) {
        val settingsConfig = configs.getType(SettingsConfiguration::class)
        val graphicsConfig = configs.getType(GraphicsConfiguration::class)

        // Set up the application
        this.app = (app as SimpleApplication).also {
            it.renderer.defaultAnisotropicFilter = 4
        }
        assetLoader = AssetLoader(this)

        // Set up camera
        this.app.flyByCamera.run {
            unregisterInput()
            isEnabled = false
        }
        this.flyByCamera = SmoothFlyByCamera(this) {
            isEnabled = true
            cameraState = CameraState.FREE_CAM
        }.apply {
            actLikeNormalFlyByCamera = !settingsConfig.isCameraSmooth
            isEnabled = !settingsConfig.startAutocamWithSong
        }
        if (settingsConfig.startAutocamWithSong) {
            with(this.app.camera) {
                CameraAngle.CAMERA_1A.let {
                    location = it.location
                    rotation = it.rotation
                }
            }
        }
        stage = with(rootNode) { +modelD("Stage.obj", "Stage.bmp") }

        /*** INIT FADE IN ***/
        fadeFilter = FadeFilter(0.5f).apply { value = 0f }

        instruments = InstrumentAssignment.assign(this, midiFile = file).onEach {
            // This is a bit of a hack to prevent stuttering when the instrument would first appear
            rootNode.attachChild(it.root)
            rootNode.detachChild(it.root)
        }
        drumSetVisibilityManager = DrumSetVisibilityManager(this, instruments.filterIsInstance<DrumSet>())

        standController = StandController(this)
        lyricController =
            LyricController(file.tracks.flatMap { it.events }.filterIsInstance<MidiTextEvent>().toMutableList(), this)
        lyricController.enabled = settingsConfig.showLyrics
        autocamController = AutoCamController(this, settingsConfig.startAutocamWithSong)
        slideCamController = SlideCameraController(this)
        debugTextController = DebugTextController(this)
        hudController = HudController(this)

        val shadowsOnly = LightingSetup.setupLights(rootNode)

        /*** SETUP SHADOWS ***/
        if (isFakeShadows) {
            shadowController = ShadowController(this)
            FilterPostProcessor(assetManager).apply {
                numSamples = GraphicsConfiguration.ANTI_ALIASING_DEFINITION[graphicsConfig.antiAliasingQuality] ?: 1
                addFilter(fadeFilter)
                addFilter(BloomFilter(BloomFilter.GlowMode.Objects))
                app.viewPort.addProcessor(this)
            }
        } else {
            FilterPostProcessor(assetManager).apply {
                val shadowDef = GraphicsConfiguration.SHADOW_DEFINITION[graphicsConfig.shadowQuality] ?: (1 to 1024)
                addFilter(
                    DirectionalLightShadowFilter(assetManager, shadowDef.second, shadowDef.first).apply {
                        light = shadowsOnly
                        isEnabled = true
                        shadowIntensity = 0.16f
                        lambda = 0.65f
                        edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
                        edgesThickness = 10
                    },
                )
                numSamples = GraphicsConfiguration.ANTI_ALIASING_DEFINITION[graphicsConfig.antiAliasingQuality] ?: 1
                addFilter(BloomFilter(BloomFilter.GlowMode.Objects))
                addFilter(fadeFilter)
                app.viewPort.addProcessor(this)
            }
            rootNode.shadowMode = RenderQueue.ShadowMode.CastAndReceive
            stage.shadowMode = RenderQueue.ShadowMode.Receive
        }

        super.initialize(stateManager, app)
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        debugTextController.tick(tpf)
    }

    /** Sets the speed of the camera, given a speed [name] and whether that key is [pressed]. */
    private fun setCameraSpeed(name: String, pressed: Boolean) = if (!pressed) {
        flyByCamera.moveSpeed = 100f
    } else {
        flyByCamera.moveSpeed =
            when (name) {
                "slow" -> 10f
                "fast" -> 200f
                else -> 100f
            }
    }

    /**
     * Handles when a key is pressed, setting the correct camera position.
     *
     * @param name The name of the key-binding pressed.
     * @param isPressed `true` if the key is pressed, `false` otherwise.
     */
    private fun handleCameraSetting(name: String, isPressed: Boolean) {
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

    /**
     * Called when an input to which this listener is registered to is invoked.
     *
     * @param name The name of the mapping that was invoked.
     * @param isPressed `true` if the action is "pressed", `false` otherwise
     * @param tpf The amount of time that has passed since the last frame, in seconds.
     */
    override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
        setCameraSpeed(name, isPressed)
        handleCameraSetting(name, isPressed)
        if (isPressed) {
            when (name) {
                "exit" -> exit()
                "debug" -> debugTextController.toggle()
                "seek_forward" -> seek(time + 10.0)
                "seek_backward" -> seek((time - 10.0).coerceAtLeast(0.0))
                "play/pause" -> togglePause()
            }
        }
    }

    /** Seeks to a given point in time. */
    abstract fun seek(time: Double)

    /** Exits the application. */
    abstract fun exit()

    /** Pauses or resumes the application, depending on the current [paused] state. */
    protected open fun togglePause() {
        paused = !paused
    }

    /** Loads and returns a reflective material with the specified [texture] at a default brightness. */
    fun reflectiveMaterial(texture: String): Material = assetLoader.reflectiveMaterial(texture)

    /** Loads and returns an unshaded material with the specified [texture]. */
    fun unshadedMaterial(texture: String): Material = assetLoader.diffuseMaterial(texture)

    fun registerEventCollector(collector: EventCollector<out MidiEvent>) {
        eventCollectors += collector
    }

    fun registerNotePeriodCollector(collector: NotePeriodCollector) {
        notePeriodCollectors += collector
    }

    fun registerNotePeriodGroupCollector(collector: NotePeriodGroupCollector) {
        notePeriodGroupCollectors += collector
    }
}
