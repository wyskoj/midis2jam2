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

package org.wysko.midis2jam2.manager.camera

import com.jme3.app.Application
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.family.animusic.SpaceLaser
import org.wysko.midis2jam2.instrument.family.brass.*
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.*
import org.wysko.midis2jam2.instrument.family.ensemble.PizzicatoStrings
import org.wysko.midis2jam2.instrument.family.ensemble.StageChoir
import org.wysko.midis2jam2.instrument.family.ensemble.StageStrings
import org.wysko.midis2jam2.instrument.family.ensemble.Timpani
import org.wysko.midis2jam2.instrument.family.ethnic.BagPipe
import org.wysko.midis2jam2.instrument.family.guitar.Banjo
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.guitar.Shamisen
import org.wysko.midis2jam2.instrument.family.organ.Accordion
import org.wysko.midis2jam2.instrument.family.organ.Harmonica
import org.wysko.midis2jam2.instrument.family.percussive.*
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.pipe.*
import org.wysko.midis2jam2.instrument.family.reed.Clarinet
import org.wysko.midis2jam2.instrument.family.reed.Oboe
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax
import org.wysko.midis2jam2.instrument.family.soundeffects.BirdTweet
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing
import org.wysko.midis2jam2.instrument.family.strings.*
import org.wysko.midis2jam2.manager.DrumSetVisibilityManager.Companion.drumSetVisibilityManagerReal
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.manager.PreferencesManager
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.state
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MOVE_SPEED = (1 / 3f)
private val WAIT_TIME = 3.seconds
private val DEFAULT_CAMERA_ANGLE = AutoCamPosition.GENERAL_A

class StandardAutoCamPlugin : AutoCamPlugin() {
    private var waiting = 0.seconds
    private var moving = false

    private val angles = mutableListOf(DEFAULT_CAMERA_ANGLE)
    private var interpolationFactor = 0f
    private var startLocation: Vector3f = DEFAULT_CAMERA_ANGLE.location.clone()
    private var startRotation: Quaternion = DEFAULT_CAMERA_ANGLE.rotation.clone()

    var currentLocation: Vector3f = DEFAULT_CAMERA_ANGLE.location.clone()
    var currentRotation: Quaternion = DEFAULT_CAMERA_ANGLE.rotation.clone()

    private lateinit var performanceManager: PerformanceManager
    private lateinit var playbackManager: PlaybackManager

    override fun initialize(app: Application) {
        performanceManager = application.stateManager.getState(PerformanceManager::class.java)
        playbackManager = application.stateManager.getState(PlaybackManager::class.java)

        if (app.state<PreferencesManager>()!!.getAppSettings().cameraSettings.isStartAutocamWithSong) {
            application.camera.location = DEFAULT_CAMERA_ANGLE.location.clone()
            application.camera.rotation = DEFAULT_CAMERA_ANGLE.rotation.clone()
        }
    }

    override fun onEnable() {
        startLocation = application.camera.location.clone()
        startRotation = application.camera.rotation.clone()
        angles.add(randomCamera(playbackManager.time))
        moving = true
        interpolationFactor = 0f
    }

    override fun update(tpf: Float) {
        if (!moving && playbackManager.time > 0.seconds) {
            waiting += tpf.toDouble().seconds

            startLocation = application.camera.location.clone()
            startRotation = application.camera.rotation.clone()
        }

        if (waiting >= WAIT_TIME) {
            waiting = Duration.ZERO

            angles.add(randomCamera(playbackManager.time))
            moving = true

            if (Math.random() < 0.2) {
                interpolationFactor = 0.99f
            }
        }

        if (moving) {
            interpolationFactor = (interpolationFactor + tpf * MOVE_SPEED).coerceAtMost(1f)
            if (interpolationFactor == 1f) {
                interpolationFactor = 0f
                moving = false

                startLocation = application.camera.location.clone()
                startRotation = application.camera.rotation.clone()
            }
        }

        application.camera.location =
            Vector3f().interpolateLocal(startLocation, angles.last().location, interpolationFactor.smooth()).also {
                currentLocation = it
            }
        application.camera.rotation =
            quaternionInterpolation(startRotation, angles.last().rotation, interpolationFactor.smooth()).also {
                currentRotation = it
            }
    }

    private fun randomCamera(time: Duration): AutoCamPosition {
        val nearEndOfPerformance = performanceManager.sequence.duration - time < WAIT_TIME * 2.5
        if (nearEndOfPerformance) {
            return DEFAULT_CAMERA_ANGLE
        }

        val selectStageCamera = Math.random() < 0.25
        return if (selectStageCamera) {
            val stageCameras = AutoCamPosition.entries.filter { it.type == AutoCamPositionType.STAGE }
            val validStageCameras = stageCameras.filter { it != angles.last() }
            validStageCameras.random()
        } else {
            val validInstrumentCameras = AutoCamPosition.entries
                .filter {
                    it.type == AutoCamPositionType.INSTRUMENT &&
                            it.pickMe.invoke(time, performanceManager.instruments, performanceManager)
                }
            val lastUsedInstrumentCameras = angles.filter { it.type == AutoCamPositionType.INSTRUMENT }
                .takeLast((performanceManager.instruments.filter { it.isVisible }.size - 2).coerceAtLeast(1))
            val notRecentlyUsedInstrumentAngles = validInstrumentCameras.minus(lastUsedInstrumentCameras.toSet())
            if (notRecentlyUsedInstrumentAngles.isNotEmpty()) {
                notRecentlyUsedInstrumentAngles.random()
            } else {
                angles.firstOrNull {
                    it.type == AutoCamPositionType.INSTRUMENT &&
                            it.pickMe(time, performanceManager.instruments, performanceManager)
                } ?: DEFAULT_CAMERA_ANGLE
            }
        }
    }

    private fun Float.smooth(): Float = if (this < 0.5) 4 * this.pow(3) else 1 - (-2 * this + 2).pow(3) / 2

    override fun cleanup(app: Application?): Unit = Unit

    override fun onDisable(): Unit = Unit

    override fun onAction(name: String?, isPressed: Boolean, tpf: Float): Unit = Unit
}

enum class AutoCamPositionType {
    INSTRUMENT,
    STAGE
}

private val alwaysTrue: (Duration, List<Instrument>, PerformanceManager) -> Boolean = { _, _, _ -> true }

/**
 * Determines if the given [instrument] class, given the list of [instruments], is visible at the given [time] and
 * visible [buffer] seconds after time.
 *
 * @param instruments the list of instruments
 * @param instrument the instrument class
 * @param time the current time
 * @param buffer the amount of time to look into the future and see if the instrument is visible
 * @return true if the instrument is visible at the given time and ahead by the buffer, false otherwise
 */
fun visibleNowAndLater(
    instruments: List<Instrument>,
    instrument: Class<out Instrument>,
    time: Duration,
    buffer: Duration
): Boolean = instruments.filterIsInstance(instrument).any {
    it.isVisible && it.calculateVisibility(time + buffer)
}

/**
 * Performs an interpolation between two quaternions.
 */
fun quaternionInterpolation(start: Quaternion, end: Quaternion, x: Float): Quaternion = Quaternion(
    start.x + (end.x - start.x) * x,
    start.y + (end.y - start.y) * x,
    start.z + (end.z - start.z) * x,
    start.w + (end.w - start.w) * x
).apply { this.normalizeLocal() }


@Suppress("KDocMissingDocumentation")
enum class AutoCamPosition(
    /** The location of the camera. */
    val location: Vector3f,
    /** The rotation of the camera. */
    val rotation: Quaternion,
    /** The condition that must be met for the camera to be picked. */
    val pickMe: (time: Duration, instruments: List<Instrument>, context: PerformanceManager) -> Boolean,
    /** The type of camera. */
    val type: AutoCamPositionType,
) {
    GENERAL_A(
        Vector3f(-2.00f, 92.00f, 134.00f),
        Quaternion(-0.00f, 0.99f, -0.16f, -0.00f),
        alwaysTrue,
        AutoCamPositionType.STAGE,
    ),

    GENERAL_B(
        Vector3f(60.00f, 92.00f, 124.00f),
        Quaternion(-0.03f, 0.97f, -0.15f, -0.18f),
        alwaysTrue,
        AutoCamPositionType.STAGE,
    ),

    GENERAL_C(
        Vector3f(-59.50f, 90.80f, 94.40f),
        Quaternion(0.03f, 0.97f, -0.18f, 0.15f),
        alwaysTrue,
        AutoCamPositionType.STAGE,
    ),

    BASS_GUITAR(
        Vector3f(0.20f, 81.10f, 32.20f),
        Quaternion(0.07f, 0.90f, -0.17f, 0.40f),
        { time, instruments, _ -> visibleNowAndLater(instruments, BassGuitar::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT,
    ),

    GUITAR(
        Vector3f(17.00f, 30.50f, 42.90f),
        Quaternion(-0.02f, 0.95f, 0.06f, 0.31f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Guitar::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT,
    ),

    DRUM_SET(
        Vector3f(-0.2f, 61.6f, 38.6f),
        Quaternion(-5.8945218E-9f, 0.9908659f, -0.13485093f, -4.3312124E-8f),
        { _, _, context -> context.drumSetVisibilityManagerReal.isVisible },
        AutoCamPositionType.INSTRUMENT,
    ),

    DRUM_SET_2(
        Vector3f(-19.6f, 78.7f, 3.8f),
        Quaternion().fromAngles(Utils.rad(27.7), Utils.rad(163.8), 0f),
        { _, _, context -> context.drumSetVisibilityManagerReal.isVisible },
        AutoCamPositionType.INSTRUMENT,
    ),

    KEYBOARDS(
        Vector3f(-32.76f, 59.79f, 38.55f),
        Quaternion(-0.06f, 0.94f, -0.27f, -0.20f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Keyboard::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT,
    ),

    KEYBOARDS_2(
        Vector3f(-35f, 76.4f, 33.6f),
        Quaternion().fromAngles(Utils.rad(55.8), Utils.rad(198.5), 0f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Keyboard::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT,
    ),

    SOPRANO_SAX(
        Vector3f(18.91f, 40.76f, -11.10f),
        Quaternion(-0.04f, 0.96f, -0.19f, -0.19f),
        { time, instruments, _ -> visibleNowAndLater(instruments, SopranoSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    ALTO_SAX(
        Vector3f(0.14f, 51.05f, -18.93f),
        Quaternion(-0.05f, 0.95f, -0.21f, -0.24f),
        { time, instruments, _ -> visibleNowAndLater(instruments, AltoSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TENOR_SAX(
        Vector3f(-1.57f, 44.77f, 43.48f),
        Quaternion(0.00f, 0.98f, -0.20f, 0.02f),
        { time, instruments, _ -> visibleNowAndLater(instruments, TenorSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BARITONE_SAX(
        Vector3f(18.66f, 58.02f, 37.77f),
        Quaternion(0.01f, 0.98f, -0.19f, 0.06f),
        { time, instruments, _ -> visibleNowAndLater(instruments, BaritoneSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    MALLETS(
        Vector3f(-13.29f, 53.30f, 86.90f),
        Quaternion(-0.02f, 0.98f, -0.18f, -0.11f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Mallets::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    MUSIC_BOX(
        Vector3f(20.54f, 15.92f, 27.50f),
        Quaternion(0.01f, 0.97f, -0.06f, 0.22f),
        { time, instruments, _ -> visibleNowAndLater(instruments, MusicBox::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TELEPHONE_RING(
        Vector3f(-10.25f, 14.42f, -30.20f),
        Quaternion(0.04f, 0.96f, -0.18f, 0.22f),
        { time, instruments, _ -> visibleNowAndLater(instruments, TelephoneRing::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    SPACE_LASER(
        Vector3f(-32.91f, 1.40f, 4.15f),
        Quaternion(-0.05f, 0.95f, 0.18f, 0.24f),
        { time, instruments, _ -> visibleNowAndLater(instruments, SpaceLaser::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    ACOUSTIC_BASS(
        Vector3f(-35.41f, 76.72f, -13.02f),
        Quaternion(-0.02f, 0.98f, -0.20f, -0.09f),
        { time, instruments, _ -> visibleNowAndLater(instruments, AcousticBass::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    VIOLIN(
        Vector3f(6.86f, 67.31f, 24.77f),
        Quaternion(0.01f, 0.99f, -0.12f, 0.05f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Violin::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    VIOLA(
        Vector3f(-10.58f, 39.79f, 17.41f),
        Quaternion(0.02f, 0.98f, -0.18f, 0.12f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Viola::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    CELLO(
        Vector3f(-49.57f, 62.85f, 10.76f),
        Quaternion(-0.03f, 0.97f, -0.21f, -0.15f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Cello::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    HARP(
        Vector3f(-70.15f, 78.19f, 33.63f),
        Quaternion(-0.06f, 0.94f, -0.18f, -0.29f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Harp::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    CHOIR(
        Vector3f(28.63f, 74.08f, -7.62f),
        Quaternion(0.01f, 0.99f, -0.10f, 0.08f),
        { time, instruments, _ -> visibleNowAndLater(instruments, StageChoir::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TUBULAR_BELLS(
        Vector3f(-57.10f, 95.29f, -52.64f),
        Quaternion(-0.01f, 0.99f, -0.10f, -0.05f),
        { time, instruments, _ -> visibleNowAndLater(instruments, TubularBells::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_1(
        Vector3f(-76.30f, 76.00f, -57.11f),
        Quaternion(-0.02f, 0.98f, -0.09f, -0.19f),
        { time, instruments, _ ->
            instruments.filterIsInstance<StageStrings>().any { it.isVisible } && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_2(
        Vector3f(-68.73f, 81.60f, -51.70f),
        Quaternion(-0.05f, 0.92f, -0.12f, -0.38f),
        { time, instruments, _ ->
            instruments.filterIsInstance<StageStrings>().count { it.isVisible } >= 2 && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_3_PLUS(
        Vector3f(-34.77f, 87.41f, -13.06f),
        Quaternion(-0.06f, 0.86f, -0.10f, -0.49f),
        { time, instruments, _ ->
            instruments.filterIsInstance<StageStrings>().count { it.isVisible } >= 3 && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_HORNS(
        Vector3f(-52.16f, 67.44f, -51.44f),
        Quaternion(-0.01f, 0.99f, -0.09f, -0.07f),
        { time, instruments, _ -> visibleNowAndLater(instruments, StageHorns::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    PIZZICATO_STRINGS(
        Vector3f(-68.83f, 56.76f, -52.56f),
        Quaternion(-0.05f, 0.95f, -0.24f, -0.19f),
        { time, instruments, _ ->
            visibleNowAndLater(
                instruments,
                PizzicatoStrings::class.java,
                time,
                WAIT_TIME * 1.5
            )
        },
        AutoCamPositionType.INSTRUMENT
    ),

    ACCORDION(
        Vector3f(-55.93f, 44.35f, -16.56f),
        Quaternion(-0.03f, 0.97f, -0.16f, -0.16f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Accordion::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BANJO(
        Vector3f(32.83f, 62.14f, 46.33f),
        Quaternion(0.03f, 0.97f, -0.12f, 0.23f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Banjo::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    SHAMISEN(
        Vector3f(40.64f, 73.55f, 30.32f),
        Quaternion(0.03f, 0.97f, -0.11f, 0.24f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Shamisen::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TIMPANI(
        Vector3f(43.85f, 55.59f, -38.73f),
        Quaternion(0.01f, 0.98f, -0.21f, 0.07f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Timpani::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    MELODIC_TOM(
        Vector3f(54.01f, 83.03f, -52.03f),
        Quaternion(0.01f, 0.98f, -0.19f, 0.04f),
        { time, instruments, _ -> visibleNowAndLater(instruments, MelodicTom::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    SYNTH_DRUM(
        Vector3f(10.73f, 103.13f, -78.50f),
        Quaternion(0.06f, 0.90f, -0.13f, 0.42f),
        { time, instruments, _ -> visibleNowAndLater(instruments, SynthDrum::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TAIKO_DRUM(
        Vector3f(19.72f, 99.17f, -121.31f),
        Quaternion(0.03f, 0.91f, -0.07f, 0.42f),
        { time, instruments, _ -> visibleNowAndLater(instruments, TaikoDrum::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TROMBONE(
        Vector3f(28.212189f, 86.88116f, 41.177956f),
        Quaternion(0.026606327f, 0.97332513f, -0.1550696f, 0.16698427f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Trombone::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    FLUTE(
        Vector3f(5.84f, 54.35f, 9.74f),
        Quaternion(-0.00f, 1.00f, -0.06f, -0.03f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Flute::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    PICCOLO(
        Vector3f(5.84f, 60.25f, 9.74f),
        Quaternion(-0.00f, 1.00f, -0.06f, -0.03f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Piccolo::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    RECORDER(
        Vector3f(-4.77f, 48.54f, 9.77f),
        Quaternion(0.01f, 0.99f, -0.07f, 0.09f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Recorder::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    HARMONICA(
        Vector3f(56.07f, 38.10f, -30.10f),
        Quaternion(0.10f, 0.86f, -0.18f, 0.47f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Harmonica::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    PAN_FLUTE(
        Vector3f(58.05f, 37.40f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.16f, 0.18f),
        { time, instruments, _ -> visibleNowAndLater(instruments, PanFlute::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    WHISTLES(
        Vector3f(58.05f, 37.40f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.20f, 0.17f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Whistles::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BLOWN_BOTTLE(
        Vector3f(58.05f, 31.78f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.17f, 0.16f),
        { time, instruments, _ -> visibleNowAndLater(instruments, BlownBottle::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    AGOGOS(
        Vector3f(55.97f, 32.62f, 11.38f),
        Quaternion(0.02f, 0.98f, -0.17f, 0.11f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Agogos::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    WOODBLOCKS(
        Vector3f(54.60f, 29.79f, 17.34f),
        Quaternion(0.01f, 0.99f, -0.13f, 0.11f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Woodblocks::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TUBA(
        Vector3f(-75.00f, 33.61f, 9.40f),
        Quaternion(-0.02f, 0.97f, -0.11f, -0.22f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Tuba::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    FRENCH_HORN(
        Vector3f(-82.14993f, 43.444687f, 30.638006f),
        Quaternion(-0.05368753f, 0.9447794f, -0.19924761f, -0.2545779f),
        { time, instruments, _ -> visibleNowAndLater(instruments, FrenchHorn::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TRUMPET(
        Vector3f(-0.17f, 61.50f, 30.30f),
        Quaternion(-0.01f, 0.93f, -0.03f, -0.37f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Trumpet::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    OBOE(
        Vector3f(18.71f, 53.21f, 35.69f),
        Quaternion(-0.01f, 0.98f, -0.17f, -0.05f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Oboe::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    CLARINET(
        Vector3f(-13.75f, 52.54f, 37.02f),
        Quaternion(-0.01f, 0.99f, -0.15f, -0.04f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Clarinet::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    STEEL_DRUMS(
        Vector3f(46.01f, 62.34f, -29.49f),
        Quaternion(0.02f, 0.97f, -0.18f, 0.13f),
        { time, instruments, _ -> visibleNowAndLater(instruments, SteelDrums::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    FIDDLE(
        Vector3f(-7.67f, 79.05f, 19.95f),
        Quaternion(-0.01f, 0.98f, -0.18f, -0.04f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Fiddle::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    OCARINA(
        Vector3f(36.71f, 54.71f, 33.44f),
        Quaternion(0.04f, 0.96f, -0.18f, 0.21f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Ocarina::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BAG_PIPE(
        Vector3f(-49.76239f, 33.13658f, 82.276375f),
        Quaternion(-0.0023348634f, 0.9805548f, -0.011687864f, -0.19588324f),
        { time, instruments, _ -> visibleNowAndLater(instruments, BagPipe::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    KALIMBA(
        Vector3f(20.016724f, 50.83171f, 53.29627f),
        Quaternion(-0.0011894251f, 0.9525223f, -0.30444378f, -0.0037213443f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Kalimba::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TINKLE_BELL(
        Vector3f(34.142365f, 50.831703f, 54.312134f),
        Quaternion(0.024830274f, 0.94041014f, -0.33174983f, 0.070386335f),
        { time, instruments, _ -> visibleNowAndLater(instruments, TinkleBell::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BIRD_TWEET(
        Vector3f(126.65341f, 55.186848f, -5.279584f),
        Quaternion(0.027039362f, 0.9644452f, -0.1090079f, 0.2392313f),
        { time, instruments, _ -> visibleNowAndLater(instruments, BirdTweet::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),
}
