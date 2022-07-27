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
import kotlinx.coroutines.runBlocking
import org.wysko.midis2jam2.gui.QualityLevel
import org.wysko.midis2jam2.gui.antiAliasingDefinition
import org.wysko.midis2jam2.gui.shadowDefinition
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.*
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.Mallets
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.MusicBox
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.TubularBells
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani
import org.wysko.midis2jam2.instrument.family.guitar.Banjo
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.guitar.Shamisen
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.percussion.Percussion
import org.wysko.midis2jam2.instrument.family.percussive.*
import org.wysko.midis2jam2.instrument.family.piano.FifthsKeyboard
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.pipe.*
import org.wysko.midis2jam2.instrument.family.reed.Clarinet
import org.wysko.midis2jam2.instrument.family.reed.Oboe
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax
import org.wysko.midis2jam2.instrument.family.soundeffects.BirdTweet
import org.wysko.midis2jam2.instrument.family.soundeffects.Helicopter
import org.wysko.midis2jam2.instrument.family.soundeffects.ReverseCymbal
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing
import org.wysko.midis2jam2.instrument.family.strings.*
import org.wysko.midis2jam2.midi.*
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.*
import java.util.*
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

    val fakeShadows = properties.getProperty("shadows").equals("none", ignoreCase = true)

    @Suppress("KDocMissingDocumentation")
    override fun initialize(stateManager: AppStateManager, app: Application) {
        this.app = app as SimpleApplication
        this.assetLoader = AssetLoader(this)

        this.app.renderer.defaultAnisotropicFilter = 4

        /*** CONFIGURE CAMERA ***/
        this.app.flyByCamera.unregisterInput()
        this.flyByCamera = FlyByCameraListenable(this.app.camera) {
            autocamController.enabled = false
        }
        this.flyByCamera.registerWithInput(this.app.inputManager)
        this.flyByCamera.moveSpeed = 100f
        this.flyByCamera.zoomSpeed = -10f
        this.flyByCamera.isEnabled = true
        this.flyByCamera.isDragToRotate = true
        this.app.camera.fov = 50f

        /*** LOAD STAGE ***/
        stage = loadModel("Stage.obj", "Stage.bmp").also {
            rootNode.attachChild(it)
        }

        /*** INIT FADE IN ***/
        fade = FadeFilter(0.5f).apply {
            value = 0f
        }

        /*** INITIALIZE INSTRUMENTS ***/
        runBlocking {
            instruments = Array(16) { ArrayList<MidiChannelSpecificEvent>() }.apply {
                /* For each track, add each of its events to the corresponding channel's list */
                file.tracks.forEach { (events) ->
                    events.filterIsInstance<MidiChannelSpecificEvent>().forEach {
                        this[it.channel].add(it)
                    }
                }
            }.onEach { channel ->
                /* Sort each channel by event times */
                channel.sortBy { it.time }
            }.foldIndexed(ArrayList()) { channelNumber, acc, events -> // pure magic
                if (channelNumber == 9) { // Percussion channel
                    if (events.filterIsInstance<MidiNoteOnEvent>().isNotEmpty()) {
                        acc.apply {
                            add(Percussion(this@Midis2jam2, events))
                        }
                    } else {
                        acc
                    }
                } else { // Melodic channel
                    val programEvents = events.filterIsInstance<MidiProgramEvent>().let {
                        it.ifEmpty {
                            listOf(MidiProgramEvent(0, channelNumber, 0))
                        }
                    }.also {
                        MidiProgramEvent.removeDuplicateProgramEvents(it as MutableList<MidiProgramEvent>)
                    }
                    if (programEvents.size == 1) {
                        acc.apply {
                            fromEvents(programEvents.first().programNum, events)?.let { add(it) }
                        }
                    } else {
                        val lastProgramPerNote = HashMap<Int, ArrayList<MidiChannelSpecificEvent>>().apply {
                            programEvents.forEach { programEvent ->
                                this[programEvent.programNum] = ArrayList()
                            }
                        }
                        val noteOnPrograms = HashMap<Int, MidiProgramEvent>()
                        var warned = false
                        events.forEach { event ->
                            if (event !is MidiNoteOffEvent) {
                                for (i in programEvents.indices) {
                                    if (i == programEvents.size - 1
                                        || event.time in programEvents[i].time until programEvents[i + 1].time
                                    ) {
                                        lastProgramPerNote[programEvents[i].programNum]?.add(event)
                                        if (event is MidiNoteOnEvent) {
                                            noteOnPrograms[event.note] = programEvents[i]
                                        }
                                        break
                                    }
                                }
                            } else {
                                try {
                                    @Suppress("ReplaceNotNullAssertionWithElvisReturn")
                                    lastProgramPerNote[noteOnPrograms[event.note]!!.programNum]!!.add(event)
                                } catch (npe: NullPointerException) {
                                    if (!warned) {
                                        logger().warn("Unbalanced note on/off events.")
                                        warned = true
                                    }
                                }
                            }
                        }
                        acc.apply {
                            lastProgramPerNote.entries.forEach {
                                fromEvents(it.key, it.value)?.let { it1 -> add(it1) }
                            }
                        }
                    }
                }
            }
        }
        standController = StandController(this)
        lyricController = LyricController(file.tracks.flatMap { it.events }.filterIsInstance<MidiTextEvent>().toMutableList(), this)
        lyricController.enabled = properties.getProperty("lyrics") == "true"

        autocamController = AutoCamController(this, properties.getProperty("auto_autocam") == "true")

        currentCameraAngle = CameraAngle.CAMERA_1A

        debugTextController = DebugTextController(this)
        hudController = HudController(this)

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
                addFilter(DirectionalLightShadowFilter(assetManager, shadowDef.second, shadowDef.first).apply {
                    light = shadowsOnly
                    isEnabled = true
                    shadowIntensity = 0.16f
                    lambda = 0.65f
                    edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
                    edgesThickness = 10
                })
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

    lateinit var assetLoader: AssetLoader

    /** The fly-by camera. */
    lateinit var flyByCamera: FlyByCameraListenable

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

    /** The root node of the scene. */
    val rootNode: Node = Node("root")

    /** The stage of the scene. */
    lateinit var stage: Spatial

    /** The amount of time that has passed since the start of the song (or the time until the start). */
    var timeSinceStart: Double = -2.0
        protected set

    /** On-screen text for debugging. */
    private lateinit var debugTextController: DebugTextController

    /** The version of midis2jam2. */
    val version: String = Utils.resourceToString("/version.txt")

    /** The build information of midis2jam2. */
    val build: String = Utils.resourceToString("/build.txt")

    /** The fade filter. */
    protected lateinit var fade: FadeFilter

    /** The filter post processor. */
    protected lateinit var fpp: FilterPostProcessor

    /**
     * True if enhanced graphics should be used, false otherwise.
     */
    val enhancedGraphics: Boolean = true

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

    /** The current camera position. */
    private var currentCameraAngle: CameraAngle = CameraAngle.CAMERA_1A
        set(value) {
            app.camera.location = value.location
            app.camera.rotation = value.rotation
            field = value
        }

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

    /**
     * When the MIDI sequence ends, the [timeSinceStart] is recorded to this variable to know when to close the
     * app (three seconds after the end).
     */
    protected var stopTime: Double by Delegates.notNull()

    /**
     * True if the sequencer has reached the end of the MIDI file, false otherwise.
     */
    protected var afterEnd: Boolean = false

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
        if (isPressed && name == "autoCam") {
            autocamController.trigger()
        }
        if (isPressed && name.startsWith("cam")) {
            currentCameraAngle = when (name) {
                "cam1" -> when (currentCameraAngle) {
                    CameraAngle.CAMERA_1A -> CameraAngle.CAMERA_1B
                    CameraAngle.CAMERA_1B -> CameraAngle.CAMERA_1C
                    else -> CameraAngle.CAMERA_1A
                }
                "cam2" -> if (currentCameraAngle == CameraAngle.CAMERA_2A) CameraAngle.CAMERA_2B else CameraAngle.CAMERA_2A
                "cam3" -> if (currentCameraAngle == CameraAngle.CAMERA_3A) CameraAngle.CAMERA_3B else CameraAngle.CAMERA_3A
                "cam4" -> if (currentCameraAngle == CameraAngle.CAMERA_4A) CameraAngle.CAMERA_4B else CameraAngle.CAMERA_4A
                "cam5" -> CameraAngle.CAMERA_5
                "cam6" -> if (currentCameraAngle == CameraAngle.CAMERA_6A) CameraAngle.CAMERA_6B else CameraAngle.CAMERA_6A
                else -> CameraAngle.CAMERA_1A // Shouldn't ever happen
            }
            autocamController.enabled = false
        }
    }

    private fun fromEvents(programNum: Int, events: ArrayList<MidiChannelSpecificEvent>): Instrument? {
        /* If there are no NoteOn events, there's no need to actually create the instrument. */
        val midiNoteEvents = events.filterIsInstance<MidiNoteEvent>()
        if (midiNoteEvents.isEmpty()) return null

        return when (programNum) {
            0 -> Keyboard(this, events, Keyboard.KeyboardSkin.PIANO)
            1 -> Keyboard(this, events, Keyboard.KeyboardSkin.BRIGHT)
            2 -> Keyboard(this, events, Keyboard.KeyboardSkin.ELECTRIC_GRAND)
            3 -> Keyboard(this, events, Keyboard.KeyboardSkin.HONKY_TONK)
            4 -> Keyboard(this, events, Keyboard.KeyboardSkin.ELECTRIC_1)
            5 -> Keyboard(this, events, Keyboard.KeyboardSkin.ELECTRIC_2)
            6 -> Keyboard(this, events, Keyboard.KeyboardSkin.HARPSICHORD)
            7 -> Keyboard(this, events, Keyboard.KeyboardSkin.CLAVICHORD)
            8 -> Keyboard(this, events, Keyboard.KeyboardSkin.CELESTA)
            9 -> Mallets(this, events, Mallets.MalletType.GLOCKENSPIEL)
            10 -> MusicBox(this, events)
            11 -> Mallets(this, events, Mallets.MalletType.VIBES)
            12 -> Mallets(this, events, Mallets.MalletType.MARIMBA)
            13 -> Mallets(this, events, Mallets.MalletType.XYLOPHONE)
            14, 98, 112 -> TubularBells(this, events)
            15, 16, 17, 18, 19, 20, 55 -> Keyboard(this, events, Keyboard.KeyboardSkin.WOOD)
            21 -> Accordion(this, events, Accordion.AccordionType.ACCORDION)
            22 -> Harmonica(this, events)
            23 -> Accordion(this, events, Accordion.AccordionType.BANDONEON)
            24, 25 -> Guitar(this, events, Guitar.GuitarType.ACOUSTIC)
            26, 27, 28, 29, 30, 31, 120 -> Guitar(this, events, Guitar.GuitarType.ELECTRIC)
            32 -> AcousticBass(this, events, AcousticBass.PlayingStyle.PIZZICATO)
            33, 34, 36, 37, 38, 39 -> BassGuitar(this, events, BassGuitar.BassGuitarType.STANDARD)
            35 -> BassGuitar(this, events, BassGuitar.BassGuitarType.FRETLESS)
            40 -> Violin(this, events)
            41 -> Viola(this, events)
            42 -> Cello(this, events)
            43 -> AcousticBass(this, events, AcousticBass.PlayingStyle.ARCO)
            44 -> StageStrings(
                this,
                events,
                StageStrings.StageStringsType.STRING_ENSEMBLE_1,
                StageStrings.StageStringBehavior.TREMOLO
            )
            48 -> StageStrings(
                this,
                events,
                StageStrings.StageStringsType.STRING_ENSEMBLE_1,
                StageStrings.StageStringBehavior.NORMAL
            )
            49 -> StageStrings(
                this,
                events,
                StageStrings.StageStringsType.STRING_ENSEMBLE_2,
                StageStrings.StageStringBehavior.NORMAL
            )
            50 -> StageStrings(
                this,
                events,
                StageStrings.StageStringsType.SYNTH_STRINGS_1,
                StageStrings.StageStringBehavior.NORMAL
            )
            51 -> StageStrings(
                this,
                events,
                StageStrings.StageStringsType.SYNTH_STRINGS_2,
                StageStrings.StageStringBehavior.NORMAL
            )
            45 -> PizzicatoStrings(this, events)
            46 -> Harp(this, events)
            47 -> Timpani(this, events)
            52 -> StageChoir(this, events, StageChoir.ChoirType.CHOIR_AAHS)
            53 -> StageChoir(this, events, StageChoir.ChoirType.VOICE_OOHS)
            54 -> StageChoir(this, events, StageChoir.ChoirType.SYNTH_VOICE)
            85 -> StageChoir(this, events, StageChoir.ChoirType.VOICE_SYNTH)
            56 -> Trumpet(this, events, Trumpet.TrumpetType.NORMAL)
            57 -> Trombone(this, events)
            58 -> Tuba(this, events)
            59 -> Trumpet(this, events, Trumpet.TrumpetType.MUTED)
            60 -> FrenchHorn(this, events)
            61 -> StageHorns(this, events, StageHorns.StageHornsType.BRASS_SECTION)
            62 -> StageHorns(this, events, StageHorns.StageHornsType.SYNTH_BRASS_1)
            63 -> StageHorns(this, events, StageHorns.StageHornsType.SYNTH_BRASS_2)
            64 -> SopranoSax(this, events)
            65 -> AltoSax(this, events)
            66 -> TenorSax(this, events)
            67 -> BaritoneSax(this, events)
            68 -> Oboe(this, events)
            71 -> Clarinet(this, events)
            72 -> Piccolo(this, events)
            73 -> Flute(this, events)
            74 -> Recorder(this, events)
            75 -> PanFlute(this, events, PanFlute.PipeSkin.WOOD)
            76 -> BlownBottle(this, events)
            78 -> Whistles(this, events)
            79 -> Ocarina(this, events)
            80 -> { // square
                if (midiNoteEvents.maxPolyphony() > 2) {
                    Keyboard(this, events, Keyboard.KeyboardSkin.SQUARE_WAVE)
                } else {
                    SpaceLaser(this, events, SpaceLaser.SpaceLaserType.SQUARE)
                }
            }
            81 -> { // sawtooth
                if (midiNoteEvents.maxPolyphony() > 2) {
                    Keyboard(this, events, Keyboard.KeyboardSkin.SAW_WAVE)
                } else {
                    SpaceLaser(this, events, SpaceLaser.SpaceLaserType.SAW)
                }
            }
            82 -> PanFlute(this, events, PanFlute.PipeSkin.GOLD) // calliope
            83 -> Keyboard(this, events, Keyboard.KeyboardSkin.CHIFF) // chiff
            84 -> Keyboard(this, events, Keyboard.KeyboardSkin.CHARANG) // charang
            86 -> FifthsKeyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // fifths
            87 -> Keyboard(this, events, Keyboard.KeyboardSkin.BASS_AND_LEAD) // bass + lead
            88 -> Keyboard(this, events, Keyboard.KeyboardSkin.NEW_AGE) // new age
            89 -> Keyboard(this, events, Keyboard.KeyboardSkin.WARM) // warm
            90 -> Keyboard(this, events, Keyboard.KeyboardSkin.POLYSYNTH) // polysynth
            91 -> Keyboard(this, events, Keyboard.KeyboardSkin.CHOIR) // choir
            92 -> StageStrings(
                this,
                events,
                StageStrings.StageStringsType.BOWED_SYNTH,
                StageStrings.StageStringBehavior.NORMAL
            ) // bowed
            93 -> Keyboard(this, events, Keyboard.KeyboardSkin.METALLIC) // metallic
            94 -> StageChoir(this, events, StageChoir.ChoirType.HALO_SYNTH) // halo
            95 -> Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // sweep
            96 -> Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // rain
            97 -> Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // soundtrack
            99 -> Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // atmosphere
            100 -> Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // brightness
            101 -> StageChoir(this, events, StageChoir.ChoirType.GOBLIN_SYNTH) // goblins
            102 -> Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // echoes
            103 -> Keyboard(this, events, Keyboard.KeyboardSkin.SYNTH) // sci-fi
            105 -> Banjo(this, events)
            106 -> Shamisen(this, events)
            110 -> Fiddle(this, events)
            113 -> Agogos(this, events)
            114 -> SteelDrums(this, events)
            115 -> Woodblocks(this, events)
            116 -> TaikoDrum(this, events)
            117 -> MelodicTom(this, events)
            118 -> SynthDrum(this, events)
            119 -> ReverseCymbal(this, events)
            121 -> StageChoir(this, events, StageChoir.ChoirType.SYNTH_VOICE)
            123 -> BirdTweet(this, events)
            124 -> TelephoneRing(this, events)
            125 -> Helicopter(this, events)
            126 -> StageChoir(this, events, StageChoir.ChoirType.SYNTH_VOICE)
            else -> null
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

    @Suppress("KDocMissingDocumentation")
    override fun update(tpf: Float) {
        super.update(tpf)
        debugTextController.tick(tpf)
    }

    /** Exits the application. */
    abstract fun exit()

    /** Loads and returns an unshaded model with the specified [model] and [texture]. */
    fun loadModel(model: String, texture: String): Spatial = assetLoader.loadDiffuseModel(model, texture)

    /** Loads and returns a reflective model with the specified [model], [texture], and [brightness]. */
    fun loadModel(model: String, texture: String, brightness: Float): Spatial =
        assetLoader.loadReflectiveModel(model, texture)

    /** Loads and returns a reflective material with the specified [texture] at a default brightness. */
    fun reflectiveMaterial(texture: String): Material = assetLoader.reflectiveMaterial(texture)

    /** Loads and returns an unshaded material with the specified [texture]. */
    fun unshadedMaterial(texture: String): Material = assetLoader.diffuseMaterial(texture)
}