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

package org.wysko.midis2jam2.world

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.Midis2jam2
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
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.pipe.*
import org.wysko.midis2jam2.instrument.family.reed.Clarinet
import org.wysko.midis2jam2.instrument.family.reed.Oboe
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.BaritoneSax
import org.wysko.midis2jam2.instrument.family.reed.sax.SopranoSax
import org.wysko.midis2jam2.instrument.family.reed.sax.TenorSax
import org.wysko.midis2jam2.instrument.family.soundeffects.TelephoneRing
import org.wysko.midis2jam2.instrument.family.strings.*
import kotlin.math.pow

/** The speed at which to transition from one camera angle to another. */
private const val MOVE_SPEED = 0.25f

/** The amount of time to wait before transitioning to the next camera angle. */
private const val WAIT_TIME = 4f

/**
 * The auto-cam controller is responsible for controlling the automatic movement of the camera. It picks camera angles
 * randomly and moves the camera to them.
 */
class AutoCamController(private val context: Midis2jam2) {

    /** When true, the auto-cam controller is enabled. */
    var enabled: Boolean = true

    /** The amount of time that has passed since the last camera angle change. */
    private var waiting = 0f

    /** True if the camera is currently moving to a new angle, false otherwise. */
    private var moving = false

    /** A list of previously used camera angles. */
    private val angles = mutableListOf<AutoCamPosition>().apply {
        add(AutoCamPosition.GENERAL_A)
    }

    /** The current amount of transition, from 0 to 1. */
    private var x = 0f

    /** The location at which the camera started at in this transition. */
    private var startLocation: Vector3f = AutoCamPosition.GENERAL_A.location.clone()

    /** The rotation at which the camera started at in this transition. */
    private var startRotation: Quaternion = AutoCamPosition.GENERAL_A.rotation.clone()

    /** Performs a tick of the auto-cam controller. */
    fun tick(time: Double, delta: Float) {
        if (!enabled) return
        context.debugText.text = buildString {
            append("enabled: $enabled\n")
            append("moving: $moving\n")
            append("waiting: $waiting\n")
            append("x: $x\n")
            append("angle: ${angles.last()}\n")
            append("time: $time\n")
            append("songLength: ${context.file.length}\n")
        }


        if (!moving && time > 0) {
            waiting += delta
            if (!angles.last().condition.invoke(time, instruments())) {
                trigger()
            }
            startLocation = context.app.camera.location.clone()
            startRotation = context.app.camera.rotation.clone()
        }

        if (waiting >= WAIT_TIME) {
            waiting = 0f
            angles.add(randomCamera(time))
            moving = true
            if (Math.random() < 0.2) {
                x = 1f
            }
        }

        if (moving) {
            if (!angles.last().condition.invoke(time, instruments())) {
                angles.add(randomCamera(time))
                x = 0f
                startLocation = context.app.camera.location.clone()
                startRotation = context.app.camera.rotation.clone()
            }
            x += delta * MOVE_SPEED
            cam.location = Vector3f().interpolateLocal(startLocation, angles.last().location, x.smooth())
            cam.rotation = quaternionInterpolation(startRotation, angles.last().rotation, x.smooth())
            if (x > 1f) {
                x = 0f
                moving = false
            }
        }
    }

    private fun randomCamera(time: Double): AutoCamPosition {
        if (context.file.length - time < WAIT_TIME * 2.5) {
            return AutoCamPosition.GENERAL_A
        }

        return if (Math.random() < 0.25) {
            // Pick a stage specific camera
            val stageCameras = AutoCamPosition.values().filter { it.type == AutoCamPositionType.STAGE }
            val validStageCameras = stageCameras.filter { it != angles.last() }
            validStageCameras.random()
        } else {
            // Pick an instrument specific camera
            val validInstrumentCameras = AutoCamPosition.values()
                .filter { it.type == AutoCamPositionType.INSTRUMENT && it.condition.invoke(time, instruments()) }
            val lastUsedInstrumentCameras = angles.filter { it.type == AutoCamPositionType.INSTRUMENT }
                .takeLast((instruments().filter { it.isVisible }.size - 2).coerceAtLeast(1))
            if (validInstrumentCameras.minus(lastUsedInstrumentCameras.toSet()).isNotEmpty()) {
                validInstrumentCameras.minus(lastUsedInstrumentCameras.toSet()).random()
            } else {
                if (validInstrumentCameras.isEmpty()) {
                    lastUsedInstrumentCameras.firstOrNull() ?: AutoCamPosition.GENERAL_A
                } else {
                    validInstrumentCameras.random()
                }
            }
        }
    }

    private fun instruments() = context.instruments.filterNotNull()

    /** Moves the camera to a new position, if it is not currently moving. */
    fun trigger() {
        enabled = true
        if (!moving) {
            waiting = WAIT_TIME
        }
    }

    /** The camera. */
    private val cam
        get() = context.app.camera
}

enum class AutoCamPositionType {
    /** Focuses on one of the instruments. */
    INSTRUMENT,

    /** Focuses on many instruments, or on the stage. */
    STAGE
}

@Suppress("KDocMissingDocumentation")
enum class AutoCamPosition(
    /** The location of the camera. */
    val location: Vector3f,
    /** The rotation of the camera. */
    val rotation: Quaternion,
    /** The condition that must be met for the camera to be triggered. Typically, the visibility of instruments will
     * determine if a camera angle is valid. */
    val condition: ((time: Double, instruments: List<Instrument>) -> Boolean),
    /** The type of camera. */
    val type: AutoCamPositionType,
) {
    GENERAL_A(
        Vector3f(-2.00f, 92.00f, 134.00f),
        Quaternion(-0.00f, 0.99f, -0.16f, -0.00f),
        { _, _ -> true },
        AutoCamPositionType.STAGE
    ),

    GENERAL_B(
        Vector3f(60.00f, 92.00f, 124.00f),
        Quaternion(-0.03f, 0.97f, -0.15f, -0.18f),
        { _, _ -> true },
        AutoCamPositionType.STAGE
    ),

    GENERAL_C(
        Vector3f(-59.50f, 90.80f, 94.40f),
        Quaternion(0.03f, 0.97f, -0.18f, 0.15f),
        { _, _ -> true },
        AutoCamPositionType.STAGE
    ),

//    OVERHEAD(
//        Vector3f(5.00f, 432.00f, 24.00f),
//        Quaternion(-0.00f, 0.75f, -0.66f, -0.00f),
//        { Math.random() < 0.2 },
//        AutoCamPositionType.STAGE
//    ),

    BASS_GUITAR(
        Vector3f(0.20f, 81.10f, 32.20f),
        Quaternion(0.07f, 0.90f, -0.17f, 0.40f),
        { time, instruments -> visibleNowAndLater(instruments, BassGuitar::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    GUITAR(
        Vector3f(17.00f, 30.50f, 42.90f),
        Quaternion(-0.02f, 0.95f, 0.06f, 0.31f),
        { time, instruments -> visibleNowAndLater(instruments, Guitar::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    SOPRANO_SAX(
        Vector3f(18.91f, 40.76f, -11.10f),
        Quaternion(-0.04f, 0.96f, -0.19f, -0.19f),
        { time, instruments -> visibleNowAndLater(instruments, SopranoSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    ALTO_SAX(
        Vector3f(0.14f, 51.05f, -18.93f),
        Quaternion(-0.05f, 0.95f, -0.21f, -0.24f),
        { time, instruments -> visibleNowAndLater(instruments, AltoSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TENOR_SAX(
        Vector3f(-1.57f, 44.77f, 43.48f),
        Quaternion(0.00f, 0.98f, -0.20f, 0.02f),
        { time, instruments -> visibleNowAndLater(instruments, TenorSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BARITONE_SAX(
        Vector3f(18.66f, 58.02f, 37.77f),
        Quaternion(0.01f, 0.98f, -0.19f, 0.06f),
        { time, instruments -> visibleNowAndLater(instruments, BaritoneSax::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),


    KEYBOARDS_1(
        Vector3f(-32.76f, 59.79f, 38.55f),
        Quaternion(-0.06f, 0.94f, -0.27f, -0.20f),
        { time, instruments -> visibleNowAndLater(instruments, Keyboard::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    MALLETS(
        Vector3f(-13.29f, 53.30f, 86.90f),
        Quaternion(-0.02f, 0.98f, -0.18f, -0.11f),
        { time, instruments -> visibleNowAndLater(instruments, Mallets::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    MUSIC_BOX(
        Vector3f(20.54f, 15.92f, 27.50f),
        Quaternion(0.01f, 0.97f, -0.06f, 0.22f),
        { time, instruments -> visibleNowAndLater(instruments, MusicBox::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TELEPHONE_RING(
        Vector3f(-10.25f, 14.42f, -30.20f),
        Quaternion(0.04f, 0.96f, -0.18f, 0.22f),
        { time, instruments -> visibleNowAndLater(instruments, TelephoneRing::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    SPACE_LASER(
        Vector3f(-32.91f, 1.40f, 4.15f),
        Quaternion(-0.05f, 0.95f, 0.18f, 0.24f),
        { time, instruments -> visibleNowAndLater(instruments, SpaceLaser::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    ACOUSTIC_BASS(
        Vector3f(-35.41f, 76.72f, -13.02f),
        Quaternion(-0.02f, 0.98f, -0.20f, -0.09f),
        { time, instruments -> visibleNowAndLater(instruments, AcousticBass::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    VIOLIN(
        Vector3f(6.86f, 67.31f, 24.77f),
        Quaternion(0.01f, 0.99f, -0.12f, 0.05f),
        { time, instruments -> visibleNowAndLater(instruments, Violin::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    VIOLA(
        Vector3f(-10.58f, 39.79f, 17.41f), Quaternion(0.02f, 0.98f, -0.18f, 0.12f), { time, instruments ->
            instruments.filterIsInstance<Viola>().any { it.isVisible }
        }, AutoCamPositionType.INSTRUMENT
    ),

    CELLO(
        Vector3f(-49.57f, 62.85f, 10.76f),
        Quaternion(-0.03f, 0.97f, -0.21f, -0.15f),
        { time, instruments -> visibleNowAndLater(instruments, Cello::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    HARP(
        Vector3f(-70.15f, 78.19f, 33.63f),
        Quaternion(-0.06f, 0.94f, -0.18f, -0.29f),
        { time, instruments -> visibleNowAndLater(instruments, Harp::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    CHOIR(
        Vector3f(28.63f, 74.08f, -7.62f),
        Quaternion(0.01f, 0.99f, -0.10f, 0.08f),
        { time, instruments -> visibleNowAndLater(instruments, StageChoir::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TUBULAR_BELLS(
        Vector3f(-57.10f, 95.29f, -52.64f),
        Quaternion(-0.01f, 0.99f, -0.10f, -0.05f),
        { time, instruments -> visibleNowAndLater(instruments, TubularBells::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_1(
        Vector3f(-76.30f, 76.00f, -57.11f), Quaternion(-0.02f, 0.98f, -0.09f, -0.19f), { time, instruments ->
            instruments.filterIsInstance<StageStrings>().any { it.isVisible } && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        }, AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_2(
        Vector3f(-68.73f, 81.60f, -51.70f), Quaternion(-0.05f, 0.92f, -0.12f, -0.38f), { time, instruments ->
            instruments.filterIsInstance<StageStrings>().count { it.isVisible } >= 2 && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        }, AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_3_PLUS(
        Vector3f(-34.77f, 87.41f, -13.06f), Quaternion(-0.06f, 0.86f, -0.10f, -0.49f), { time, instruments ->
            instruments.filterIsInstance<StageStrings>().count { it.isVisible } >= 3 && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        }, AutoCamPositionType.INSTRUMENT
    ),

    STAGE_HORNS(
        Vector3f(-52.16f, 67.44f, -51.44f),
        Quaternion(-0.01f, 0.99f, -0.09f, -0.07f),
        { time, instruments -> visibleNowAndLater(instruments, StageHorns::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    PIZZICATO_STRINGS(
        Vector3f(-68.83f, 56.76f, -52.56f),
        Quaternion(-0.05f, 0.95f, -0.24f, -0.19f),
        { time, instruments -> visibleNowAndLater(instruments, PizzicatoStrings::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    ACCORDION(
        Vector3f(-55.93f, 44.35f, -16.56f),
        Quaternion(-0.03f, 0.97f, -0.16f, -0.16f),
        { time, instruments -> visibleNowAndLater(instruments, Accordion::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BANJO(
        Vector3f(32.83f, 62.14f, 46.33f),
        Quaternion(0.03f, 0.97f, -0.12f, 0.23f),
        { time, instruments -> visibleNowAndLater(instruments, Banjo::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    SHAMISEN(
        Vector3f(40.64f, 73.55f, 30.32f),
        Quaternion(0.03f, 0.97f, -0.11f, 0.24f),
        { time, instruments -> visibleNowAndLater(instruments, Shamisen::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TIMPANI(
        Vector3f(43.85f, 55.59f, -38.73f),
        Quaternion(0.01f, 0.98f, -0.21f, 0.07f),
        { time, instruments -> visibleNowAndLater(instruments, Timpani::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    MELODIC_TOM(
        Vector3f(54.01f, 83.03f, -52.03f),
        Quaternion(0.01f, 0.98f, -0.19f, 0.04f),
        { time, instruments -> visibleNowAndLater(instruments, MelodicTom::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    SYNTH_DRUM(
        Vector3f(10.73f, 103.13f, -78.50f),
        Quaternion(0.06f, 0.90f, -0.13f, 0.42f),
        { time, instruments -> visibleNowAndLater(instruments, SynthDrum::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TAIKO_DRUM(
        Vector3f(19.72f, 99.17f, -121.31f),
        Quaternion(0.03f, 0.91f, -0.07f, 0.42f),
        { time, instruments -> visibleNowAndLater(instruments, TaikoDrum::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TROMBONE(
        Vector3f(-104.36f, 76.19f, -113.62f),
        Quaternion(0.00f, 1.00f, -0.07f, 0.07f),
        { time, instruments -> visibleNowAndLater(instruments, Trombone::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    FLUTE(
        Vector3f(5.84f, 54.35f, 9.74f),
        Quaternion(-0.00f, 1.00f, -0.06f, -0.03f),
        { time, instruments -> visibleNowAndLater(instruments, Flute::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    PICCOLO(
        Vector3f(5.84f, 60.25f, 9.74f),
        Quaternion(-0.00f, 1.00f, -0.06f, -0.03f),
        { time, instruments -> visibleNowAndLater(instruments, Piccolo::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    RECORDER(
        Vector3f(-4.77f, 48.54f, 9.77f),
        Quaternion(0.01f, 0.99f, -0.07f, 0.09f),
        { time, instruments -> visibleNowAndLater(instruments, Recorder::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    HARMONICA(
        Vector3f(56.07f, 38.10f, -30.10f),
        Quaternion(0.10f, 0.86f, -0.18f, 0.47f),
        { time, instruments -> visibleNowAndLater(instruments, Harmonica::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    PAN_FLUTE(
        Vector3f(58.05f, 37.40f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.16f, 0.18f),
        { time, instruments -> visibleNowAndLater(instruments, PanFlute::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    WHISTLES(
        Vector3f(58.05f, 37.40f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.20f, 0.17f),
        { time, instruments -> visibleNowAndLater(instruments, Whistles::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    BLOWN_BOTTLE(
        Vector3f(58.05f, 31.78f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.17f, 0.16f),
        { time, instruments -> visibleNowAndLater(instruments, BlownBottle::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    AGOGOS(
        Vector3f(55.97f, 32.62f, 11.38f),
        Quaternion(0.02f, 0.98f, -0.17f, 0.11f),
        { time, instruments -> visibleNowAndLater(instruments, Agogos::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    WOODBLOCKS(
        Vector3f(54.60f, 29.79f, 17.34f),
        Quaternion(0.01f, 0.99f, -0.13f, 0.11f),
        { time, instruments -> visibleNowAndLater(instruments, Woodblocks::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TUBA(
        Vector3f(-75.00f, 33.61f, 9.40f),
        Quaternion(-0.02f, 0.97f, -0.11f, -0.22f),
        { time, instruments -> visibleNowAndLater(instruments, Tuba::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    FRENCH_HORN(
        Vector3f(-44.77f, 41.35f, -16.86f),
        Quaternion(-0.02f, 0.97f, -0.12f, -0.20f),
        { time, instruments -> visibleNowAndLater(instruments, FrenchHorn::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    TRUMPET(
        Vector3f(-0.17f, 61.50f, 30.30f),
        Quaternion(-0.01f, 0.93f, -0.03f, -0.37f),
        { time, instruments -> visibleNowAndLater(instruments, Trumpet::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    OBOE(
        Vector3f(18.71f, 53.21f, 35.69f),
        Quaternion(-0.01f, 0.98f, -0.17f, -0.05f),
        { time, instruments -> visibleNowAndLater(instruments, Oboe::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    CLARINET(
        Vector3f(-13.75f, 52.54f, 37.02f),
        Quaternion(-0.01f, 0.99f, -0.15f, -0.04f),
        { time, instruments -> visibleNowAndLater(instruments, Clarinet::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

    PERCUSSION_A(
        Vector3f(-0.20f, 61.60f, 38.60f),
        Quaternion(-0.00f, 0.99f, -0.13f, -0.00f),
        { time, instruments -> visibleNowAndLater(instruments, Percussion::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),

//    PERCUSSION_B(
//        Vector3f(-19.60f, 78.70f, 3.80f),
//        Quaternion(0.04f, 0.95f, -0.27f, 0.14f),
//        { time, instruments -> visibleNowAndLater(instruments, Percussion::class.java, time, WAIT_TIME * 1.5) },
//        AutoCamPositionType.INSTRUMENT
//    ),

    STEEL_DRUMS(
        Vector3f(46.01f, 62.34f, -29.49f),
        Quaternion(0.02f, 0.97f, -0.18f, 0.13f),
        { time, instruments -> visibleNowAndLater(instruments, SteelDrums::class.java, time, WAIT_TIME * 1.5) },
        AutoCamPositionType.INSTRUMENT
    ),
}

fun visibleNowAndLater(
    instruments: List<Instrument>,
    instrument: Class<out Instrument>,
    time: Double,
    buffer: Number
): Boolean =
    instruments.filterIsInstance(instrument).any {
        it.isVisible && it.calcVisibility(time + buffer.toDouble(), future = true)
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


/**
 * Applies cubic-ease-in-out interpolation to a value.
 */
fun Float.smooth(): Float = if (this < 0.5) 4 * this.pow(3) else 1 - (-2 * this + 2).pow(3) / 2