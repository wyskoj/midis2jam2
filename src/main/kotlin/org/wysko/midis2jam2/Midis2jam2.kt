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

package org.wysko.midis2jam2

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.input.controls.ActionListener
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.BloomFilter
import com.jme3.post.filters.FadeFilter
import com.jme3.post.ssao.SSAOFilter
import com.jme3.renderer.RenderManager
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.shadow.DirectionalLightShadowFilter
import com.jme3.shadow.EdgeFilteringMode
import org.wysko.midis2jam2.gui.QualityLevel
import org.wysko.midis2jam2.gui.antiAliasingDefinition
import org.wysko.midis2jam2.gui.shadowDefinition
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.InstrumentAssignment
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.midi.MidiTextEvent
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.world.AssetLoader
import org.wysko.midis2jam2.world.AutoCamController
import org.wysko.midis2jam2.world.CameraAngle
import org.wysko.midis2jam2.world.DebugTextController
import org.wysko.midis2jam2.world.FlyByCameraListenable
import org.wysko.midis2jam2.world.HudController
import org.wysko.midis2jam2.world.LyricController
import org.wysko.midis2jam2.world.ShadowController
import org.wysko.midis2jam2.world.StandController
import java.util.Properties
import kotlin.properties.Delegates

/**
 * Acts as the main animation and playback class.
 *
 * @param file the parsed MIDI file
 * @param properties the settings
 * @constructor Create an instance of midis2jam2
 */
@Suppress("LeakingThis")
abstract class Midis2jam2(
    val file: MidiFile,
    val properties: Properties
) : AbstractAppState(), ActionListener {

    /** The JME3 application that created this. */
    lateinit var app: SimpleApplication

    /** The asset manager. */
    val assetManager: AssetManager by lazy {
        app.assetManager
    }

    /** The render manager. */
    val renderManager: RenderManager by lazy {
        app.renderManager
    }

    /** The asset loader. */
    lateinit var assetLoader: AssetLoader

    /** The fade filter. */
    protected lateinit var fade: FadeFilter

    /** The filter post processor. */
    private lateinit var fpp: FilterPostProcessor

    /** The root node of the scene. */
    val rootNode: Node = Node("root")

    /** The stage of the scene. */
    lateinit var stage: Spatial

    /** True if the current shadow configuration requires "fake" shadows to be used, false otherwise. */
    val fakeShadows: Boolean = properties.getProperty("shadows").equals("none", ignoreCase = true)

    /** True if enhanced graphics should be used, false otherwise. */
    val enhancedGraphics: Boolean = true

    /** The amount of time that has passed since the start of the song (or the time until the start). */
    var timeSinceStart: Double = -2.0
        protected set

    /**
     * When the MIDI sequence ends, the [timeSinceStart] is recorded to this variable to know when to close the
     * app (three seconds after the end).
     */
    protected var stopTime: Double by Delegates.notNull()

    /**
     * True if the sequencer has reached the end of the MIDI file, false otherwise.
     */
    protected var afterEnd: Boolean = false

    /** The version of midis2jam2. */
    val version: String = Utils.resourceToString("/version.txt")

    /** The build information of midis2jam2. */
    val build: String = Utils.resourceToString("/build.txt")

    /** The shadow controller. */
    var shadowController: ShadowController? = null

    /** The stand controller. */
    lateinit var standController: StandController

    /** The lyric controller. */
    lateinit var lyricController: LyricController

    /** The autocam controller. */
    lateinit var autocamController: AutoCamController

    /** The HUD controller. */
    lateinit var hudController: HudController

    /** On-screen text for debugging. */
    private lateinit var debugTextController: DebugTextController

    /** The fly-by camera. */
    private lateinit var flyByCamera: FlyByCameraListenable

    /** The current camera position. */
    private var currentCameraAngle: CameraAngle = CameraAngle.CAMERA_1A
        set(value) {
            app.camera.location = value.location
            app.camera.rotation = value.rotation
            field = value
        }

    /**
     * The instruments.
     *
     * To form the list of the instruments, the MIDI file is read and program events are calculated, appropriately
     * creating instances of each instrument and assigning the correct events to respective instruments.
     *
     * The method used to calculate instruments is different from that in MIDIJam. In MIDIJam, when a channel would
     * switch programs, it would spawn a new instrument every time. For example, if a channel played 8th notes and
     * switched between two different instruments on each note, a new instrument would be spawned for each note.
     *
     * This method consolidates duplicate instrument types to reduce bloating. That is, if two program events in this
     * channel appear, containing the same program number, the events that occur in each will be merged into a single
     * instrument.
     *
     * Because it is possible for a program event to occur in between a note on event and the corresponding note off
     * event, the method keeps track of which instrument each note on event occurs on. Then, when a note off event
     * occurs, rather than checking the last program event to determine which instrument it should apply to, it applies
     * it to the instrument of the last note on with the same note value.
     */
    lateinit var instruments: List<Instrument>

    @Suppress("KDocMissingDocumentation")
    override fun initialize(stateManager: AppStateManager, app: Application) {
        this.app = app as SimpleApplication
        this.assetLoader = AssetLoader(this)

        this.app.renderer.defaultAnisotropicFilter = 4

        /*** CONFIGURE CAMERA ***/
        this.app.flyByCamera.unregisterInput()
        this.flyByCamera = FlyByCameraListenable(this.app.camera) {
            autocamController.enabled = false
        }.apply {
            registerWithInput(this@Midis2jam2.app.inputManager)
            moveSpeed = 100f
            zoomSpeed = -10f
            isEnabled = true
            isDragToRotate = true
        }
        this.app.camera.fov = 50f
        if (properties.getProperty("record_lock")?.equals("true") == true) this.flyByCamera.isEnabled = false

        /*** LOAD STAGE ***/
        stage = loadModel("Stage.obj", "Stage.bmp").also {
            rootNode.attachChild(it)
        }

        /*** INIT FADE IN ***/
        fade = FadeFilter(0.5f).apply {
            value = 0f
        }

        instruments = InstrumentAssignment.assign(this, midiFile = file)

        standController = StandController(this)
        lyricController =
            LyricController(file.tracks.flatMap { it.events }.filterIsInstance<MidiTextEvent>().toMutableList(), this)
        lyricController.enabled = properties.getProperty("lyrics") == "true"
        autocamController = AutoCamController(this, properties.getProperty("auto_autocam") == "true")
        debugTextController = DebugTextController(this)
        hudController = HudController(this)

        currentCameraAngle = CameraAngle.CAMERA_1A

        /*** SETUP LIGHTS ***/
        val shadowsOnly = DirectionalLight().apply { // No light effects (shadows only)
            color = ColorRGBA.Black
            direction = Vector3f(0.1f, -1f, -0.1f)
            rootNode.addLight(this)
        }
        DirectionalLight().apply { // Main light
            color = ColorRGBA(0.9f, 0.9f, 0.9f, 1f)
            direction = Vector3f(0f, -1f, -1f)
            rootNode.addLight(this)
        }
        DirectionalLight().apply { // Backlight
            color = ColorRGBA(0.1f, 0.1f, 0.3f, 1f)
            direction = Vector3f(0f, 1f, 1f)
            rootNode.addLight(this)
        }
        AmbientLight().apply { // Ambience
            color = ColorRGBA(0.5f, 0.5f, 0.5f, 1f)
            rootNode.addLight(this)
        }

        /*** SETUP SHADOWS ***/
        if (fakeShadows) {
            shadowController = ShadowController(this)
            fpp = FilterPostProcessor(assetManager).apply {
                numSamples =
                    antiAliasingDefinition.getOrDefault(QualityLevel.valueOf(properties.getProperty("antialiasing")), 1)
                addFilter(fade)
                addFilter(BloomFilter(BloomFilter.GlowMode.Objects))
                app.viewPort.addProcessor(this)
            }
        } else {
            fpp = FilterPostProcessor(assetManager).apply {
                val shadowDef = shadowDefinition.getOrDefault(
                    QualityLevel.valueOf(properties.getProperty("shadows")),
                    1 to 1024
                )
                addFilter(
                    DirectionalLightShadowFilter(assetManager, shadowDef.second, shadowDef.first).apply {
                        light = shadowsOnly
                        isEnabled = true
                        shadowIntensity = 0.16f
                        lambda = 0.65f
                        edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
                        edgesThickness = 10
                    }
                )
                numSamples =
                    antiAliasingDefinition.getOrDefault(QualityLevel.valueOf(properties.getProperty("antialiasing")), 1)
                addFilter(BloomFilter(BloomFilter.GlowMode.Objects))
                addFilter(SSAOFilter())
                addFilter(fade)
                app.viewPort.addProcessor(this)
            }
            rootNode.shadowMode = RenderQueue.ShadowMode.CastAndReceive
            stage.shadowMode = RenderQueue.ShadowMode.Receive
        }

        super.initialize(stateManager, app)
    }

    @Suppress("KDocMissingDocumentation")
    override fun update(tpf: Float) {
        super.update(tpf)
        debugTextController.tick(tpf)
    }

    /** Sets the speed of the camera, given a speed [name] and whether that key is [pressed]. */
    private fun setCameraSpeed(name: String, pressed: Boolean) = if (!pressed) {
        flyByCamera.moveSpeed = 100f
    } else {
        flyByCamera.moveSpeed = when (name) {
            "slow" -> 10f
            "fast" -> 200f
            else -> 100f
        }
    }

    /**
     * Handles when a key is pressed, setting the correct camera position.
     *
     * @param name      the name of the key bind pressed
     * @param isPressed is key pressed?
     */
    private fun handleCameraSetting(name: String, isPressed: Boolean) {
        if (!isPressed) return
        if (name == "autoCam") {
            autocamController.trigger()
        }
        if (name.startsWith("cam")) {
            currentCameraAngle = CameraAngle.handleCameraAngle(currentCameraAngle, name)
            autocamController.enabled = false
        }
    }

    /**
     * Called when an input to which this listener is registered to is invoked.
     *
     * @param name The name of the mapping that was invoked
     * @param isPressed True if the action is "pressed", false otherwise
     * @param tpf The time per frame value.
     */
    override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
        if (properties.getProperty("record_lock")?.equals("true") == true) return // Input lock when recording
        setCameraSpeed(name, isPressed)
        handleCameraSetting(name, isPressed)
        if (isPressed) {
            when (name) {
                "exit" -> {
                    exit()
                }
                "debug" -> {
                    debugTextController.toggle()
                }
            }
        }
    }

    /** Exits the application. */
    abstract fun exit()

    /** Loads and returns an unshaded model with the specified [model] and [texture]. */
    fun loadModel(model: String, texture: String): Spatial = assetLoader.loadDiffuseModel(model, texture)

    /** Loads and returns a reflective model with the specified [model], [texture], and [brightness]. */
    fun loadModel(model: String, texture: String, @Suppress("UNUSED_PARAMETER") brightness: Float): Spatial =
        assetLoader.loadReflectiveModel(model, texture)

    /** Loads and returns a reflective material with the specified [texture] at a default brightness. */
    fun reflectiveMaterial(texture: String): Material = assetLoader.reflectiveMaterial(texture)

    /** Loads and returns an unshaded material with the specified [texture]. */
    fun unshadedMaterial(texture: String): Material = assetLoader.diffuseMaterial(texture)
}
