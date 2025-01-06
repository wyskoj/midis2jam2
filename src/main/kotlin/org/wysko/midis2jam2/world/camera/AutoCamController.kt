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

package org.wysko.midis2jam2.world.camera

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.Midis2jam2
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
import org.wysko.midis2jam2.starter.configuration.SettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.getType
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/** The speed at which to transition from one camera angle to another. */
private const val MOVE_SPEED = (1 / 3f)

/** The amount of time to wait before transitioning to the next camera angle. */
private val WAIT_TIME = 3.seconds

/**
 * The auto-cam controller is responsible for controlling the automatic movement of the camera. It picks camera angles
 * randomly and moves the camera to them.
 */
class AutoCamController(private val context: Midis2jam2, startEnabled: Boolean) {

    /** When true, the auto-cam controller is enabled. */
    var enabled: Boolean = startEnabled
        set(value) {
            if (value && !field) { // If the field is being set true from a false state
                startLocation = context.app.camera.location.clone()
                startRotation = context.app.camera.rotation.clone()
            }
            field = value
        }

    /** The amount of time that has passed since the last camera angle change. */
    private var waiting = 0.seconds

    /** True if the camera is currently moving to a new angle, false otherwise. */
    private var moving = false

    /** A list of previously used camera angles. */
    private val angles = mutableListOf(AutoCamPosition.GENERAL_A)

    /** The current amount of transition, from 0 to 1. */
    private var x = 0f

    /** The location at which the camera started at in this transition. */
    private var startLocation: Vector3f = AutoCamPosition.GENERAL_A.location.clone()

    /** The rotation at which the camera started at in this transition. */
    private var startRotation: Quaternion = AutoCamPosition.GENERAL_A.rotation.clone()

    /** The location at which the camera started at in this transition. */
    var currentLocation: Vector3f = AutoCamPosition.GENERAL_A.location.clone()

    /** The rotation at which the camera started at in this transition. */
    var currentRotation: Quaternion = AutoCamPosition.GENERAL_A.rotation.clone()

    /**
     * Applies cubic-ease-in-out interpolation to a value unless classic camera is enabled.
     */
    fun Float.smooth(): Float = if (context.configs.getType(SettingsConfiguration::class).isClassicCamera) {
        this
    } else {
        if (this < 0.5) 4 * this.pow(3) else 1 - (-2 * this + 2).pow(3) / 2
    }

    /** Performs a tick of the auto-cam controller. */
    fun tick(time: Duration, delta: Duration): Boolean {
        if (!enabled) return false

        /* If the camera is not moving, and the song has started, */
        if (!moving && time > 0.seconds) {
            /* Increment the waiting timer */
            waiting += delta

            /* If the instrument dictates that it should no longer be focused on, */
            if (!angles.last().stayHere.invoke(time, context.instruments, context)) {
                /* Pick a new angle */
                trigger()
            }

            /* Copy down the current location and rotation so that we can do some interpolation */
            startLocation = context.app.camera.location.clone()
            startRotation = context.app.camera.rotation.clone()
        }

        /* If we have waited longer than the wait time, */
        if (waiting >= WAIT_TIME) {
            /* Reset the waiting timer */
            waiting = Duration.ZERO

            /* Pick a new camera angle */
            angles.add(randomCamera(time))

            /* We are now moving */
            moving = true

            /* About 1/5 of the time, do not interpolate and just move to the new angle (jump-cut) */
            if (context.configs.getType(SettingsConfiguration::class).isClassicCamera) {
                if (Math.random() < 0.4) {
                    x = 1f
                }
            } else {
                if (Math.random() < 0.2) {
                    x = 1f
                }
            }
        }

        /* If we are in the process of moving to a new camera angle, */
        if (moving) {
            /* Increment interpolation index */
            x += (delta.toDouble(DurationUnit.SECONDS) * MOVE_SPEED).toFloat()



            /* If we have reached the end of the interpolation, */
            if (x > 1f) {
                /* Reset the interpolation index */
                x = 0f

                /* We are no longer moving */
                moving = false

                startLocation = context.app.camera.location.clone()
                startRotation = context.app.camera.rotation.clone()
            }
        }

        /* Set the camera location and rotation to the interpolated values */
        cam.location = Vector3f().interpolateLocal(startLocation, angles.last().location, x.smooth()).also {
            currentLocation = it
        }
        cam.rotation = quaternionInterpolation(startRotation, angles.last().rotation, x.smooth()).also {
            currentRotation = it
        }

        return true
    }

    private fun randomCamera(time: Duration): AutoCamPosition {
        /* If we are near the end of the song, */
        if (context.sequence.duration - time < WAIT_TIME * 2.5) {
            /* Pick GENERAL_A */
            return AutoCamPosition.GENERAL_A
        }

        /* About 1/4 of the time, pick a stage angle */
        return if (Math.random() < 0.25) {
            /* Collect all stage camera angles */
            val stageCameras = AutoCamPosition.values().filter { it.type == AutoCamPositionType.STAGE }

            /* Valid stage cameras are those that are not the current one */
            val validStageCameras = stageCameras.filter { it != angles.last() }

            /* Pick a random valid stage camera */
            validStageCameras.random()
        } else {
            /* Collect all valid instrument camera angles */
            val validInstrumentCameras = AutoCamPosition.values()
                .filter { it.type == AutoCamPositionType.INSTRUMENT && it.pickMe.invoke(time, context.instruments, context) }

            /* Collect some the last used instrument camera angles */
            val lastUsedInstrumentCameras = angles.filter { it.type == AutoCamPositionType.INSTRUMENT }
                .takeLast((context.instruments.filter { it.isVisible }.size - 2).coerceAtLeast(1))

            val notRecentlyUsedInstrumentAngles = validInstrumentCameras.minus(lastUsedInstrumentCameras.toSet())

            /* If there are any valid camera angles that are not the last used ones, */
            if (notRecentlyUsedInstrumentAngles.isNotEmpty()) {
                /* Pick a random camera from that list */
                notRecentlyUsedInstrumentAngles.random()
            } else {
                /* Otherwise, just pick the last used camera that has been the longest time since it was used */
                angles.firstOrNull { it.type == AutoCamPositionType.INSTRUMENT && it.pickMe(time, context.instruments, context) }
                    ?: AutoCamPosition.GENERAL_A
            }
        }
    }

    /** Moves the camera to a new position, if it is not currently moving. */
    fun trigger() {
        if (!enabled) {
            x = 0f
        }
        enabled = true

        if (!moving) {
            waiting = WAIT_TIME
        }
    }

    /** The camera. */
    private val cam
        get() = context.app.camera
}

/** Defines what type of angle an [AutoCamPosition] is. */
enum class AutoCamPositionType {
    /** Focuses on one of the instruments. */
    INSTRUMENT,

    /** Focuses on many instruments, or on the stage. */
    STAGE
}

private val alwaysTrue: (Duration, List<Instrument>, Midis2jam2) -> Boolean = { _, _, _ -> true }

@Suppress("KDocMissingDocumentation")
enum class AutoCamPosition(
    /** The location of the camera. */
    val location: Vector3f,
    /** The rotation of the camera. */
    val rotation: Quaternion,
    /** The condition that must be met for the camera to be picked. */
    val pickMe: (time: Duration, instruments: List<Instrument>, context: Midis2jam2) -> Boolean,
    val stayHere: (time: Duration, instruments: List<Instrument>, context: Midis2jam2) -> Boolean,
    /** The type of camera. */
    val type: AutoCamPositionType,
) {
    GENERAL_A(
        Vector3f(-2.00f, 92.00f, 134.00f),
        Quaternion(-0.00f, 0.99f, -0.16f, -0.00f),
        alwaysTrue,
        alwaysTrue,
        AutoCamPositionType.STAGE
    ),

    GENERAL_B(
        Vector3f(60.00f, 92.00f, 124.00f),
        Quaternion(-0.03f, 0.97f, -0.15f, -0.18f),
        alwaysTrue,
        alwaysTrue,
        AutoCamPositionType.STAGE
    ),

    GENERAL_C(
        Vector3f(-59.50f, 90.80f, 94.40f),
        Quaternion(0.03f, 0.97f, -0.18f, 0.15f),
        alwaysTrue,
        alwaysTrue,
        AutoCamPositionType.STAGE
    ),

    GENERAL_D(
        Vector3f(5f, 432f, 24f),
        Quaternion().fromAngles(-1.695151f, 0f, -3.1415927f),
        { _, _, context -> context.configs.getType(SettingsConfiguration::class).isClassicCamera },
        alwaysTrue,
        AutoCamPositionType.STAGE
    ),

    BASS_GUITAR(
        Vector3f(0.20f, 81.10f, 32.20f),
        Quaternion(0.07f, 0.90f, -0.17f, 0.40f),
        { time, instruments, _ -> visibleNowAndLater(instruments, BassGuitar::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<BassGuitar>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    GUITAR(
        Vector3f(17.00f, 30.50f, 42.90f),
        Quaternion(-0.02f, 0.95f, 0.06f, 0.31f),
        { time, instruments, _ -> visibleNowAndLater(instruments, Guitar::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Guitar>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    DRUM_SET(
        Vector3f(-0.2f, 61.6f, 38.6f),
        Quaternion(-5.8945218E-9f, 0.9908659f, -0.13485093f, -4.3312124E-8f),
        { _, _, context -> context.drumSetVisibilityManager.isVisible },
        { _, _, context -> context.drumSetVisibilityManager.isVisible },
        AutoCamPositionType.INSTRUMENT
    ),

    KEYBOARDS_2(
        Vector3f(0f, 71.8f, 44.5f),
        Quaternion().fromAngles(-2.867576f, 0.7836529f, -3.1415927f),
        { time, instruments, context -> context.configs.getType(org.wysko.midis2jam2.starter.configuration.SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Keyboard::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Keyboard>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    DRUM_SET_2(
        Vector3f(-19.6f, 78.7f, 3.8f),
        Quaternion().fromAngles(-2.6581365f, -0.2827433f, -3.1415927f),
        { _, _, context -> context.configs.getType(org.wysko.midis2jam2.starter.configuration.SettingsConfiguration::class).isClassicCamera && context.drumSetVisibilityManager.isVisible },
        { _, _, context -> context.drumSetVisibilityManager.isVisible },
        AutoCamPositionType.INSTRUMENT
    ),

    BASS_GUITAR_2(
        Vector3f(35f, 25.4f, -19f),
        Quaternion().fromAngles(2.268928f, -1.0646509f, 3.0979594f),
        { time, instruments, context -> context.configs.getType(org.wysko.midis2jam2.starter.configuration.SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, BassGuitar::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<BassGuitar>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    SOPRANO_SAX(
        Vector3f(18.91f, 40.76f, -11.10f),
        Quaternion(-0.04f, 0.96f, -0.19f, -0.19f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, SopranoSax::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<SopranoSax>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),


    ALTO_SAX(
        Vector3f(0.14f, 51.05f, -18.93f),
        Quaternion(-0.05f, 0.95f, -0.21f, -0.24f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, AltoSax::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<AltoSax>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TENOR_SAX(
        Vector3f(-1.57f, 44.77f, 43.48f),
        Quaternion(0.00f, 0.98f, -0.20f, 0.02f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, TenorSax::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<TenorSax>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    BARITONE_SAX(
        Vector3f(18.66f, 58.02f, 37.77f),
        Quaternion(0.01f, 0.98f, -0.19f, 0.06f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, BaritoneSax::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<BaritoneSax>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    KEYBOARDS(
        Vector3f(-32.76f, 59.79f, 38.55f),
        Quaternion(-0.06f, 0.94f, -0.27f, -0.20f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Keyboard::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Keyboard>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    MALLETS(
        Vector3f(-13.29f, 53.30f, 86.90f),
        Quaternion(-0.02f, 0.98f, -0.18f, -0.11f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Mallets::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Mallets>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    MUSIC_BOX(
        Vector3f(20.54f, 15.92f, 27.50f),
        Quaternion(0.01f, 0.97f, -0.06f, 0.22f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, MusicBox::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<MusicBox>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TELEPHONE_RING(
        Vector3f(-10.25f, 14.42f, -30.20f),
        Quaternion(0.04f, 0.96f, -0.18f, 0.22f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, TelephoneRing::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<TelephoneRing>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    SPACE_LASER(
        Vector3f(-32.91f, 1.40f, 4.15f),
        Quaternion(-0.05f, 0.95f, 0.18f, 0.24f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, SpaceLaser::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<SpaceLaser>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    ACOUSTIC_BASS(
        Vector3f(-35.41f, 76.72f, -13.02f),
        Quaternion(-0.02f, 0.98f, -0.20f, -0.09f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, AcousticBass::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<AcousticBass>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    VIOLIN(
        Vector3f(6.86f, 67.31f, 24.77f),
        Quaternion(0.01f, 0.99f, -0.12f, 0.05f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Violin::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Violin>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    VIOLA(
        Vector3f(-10.58f, 39.79f, 17.41f),
        Quaternion(0.02f, 0.98f, -0.18f, 0.12f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Viola::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Viola>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    CELLO(
        Vector3f(-49.57f, 62.85f, 10.76f),
        Quaternion(-0.03f, 0.97f, -0.21f, -0.15f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Cello::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Cello>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    HARP(
        Vector3f(-70.15f, 78.19f, 33.63f),
        Quaternion(-0.06f, 0.94f, -0.18f, -0.29f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Harp::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Harp>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    CHOIR(
        Vector3f(28.63f, 74.08f, -7.62f),
        Quaternion(0.01f, 0.99f, -0.10f, 0.08f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, StageChoir::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<StageChoir>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TUBULAR_BELLS(
        Vector3f(-57.10f, 95.29f, -52.64f),
        Quaternion(-0.01f, 0.99f, -0.10f, -0.05f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, TubularBells::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<TubularBells>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_1(
        Vector3f(-76.30f, 76.00f, -57.11f),
        Quaternion(-0.02f, 0.98f, -0.09f, -0.19f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera &&
            instruments.filterIsInstance<StageStrings>().any { it.isVisible } && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        },
        { _, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && instruments.filterIsInstance<StageStrings>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_2(
        Vector3f(-68.73f, 81.60f, -51.70f),
        Quaternion(-0.05f, 0.92f, -0.12f, -0.38f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera &&
            instruments.filterIsInstance<StageStrings>().count { it.isVisible } >= 2 && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        },
        { _, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && instruments.filterIsInstance<StageStrings>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_STRINGS_3_PLUS(
        Vector3f(-34.77f, 87.41f, -13.06f),
        Quaternion(-0.06f, 0.86f, -0.10f, -0.49f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera &&
            instruments.filterIsInstance<StageStrings>().count { it.isVisible } >= 3 && visibleNowAndLater(
                instruments,
                StageStrings::class.java,
                time,
                WAIT_TIME
            )
        },
        { _, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && instruments.filterIsInstance<StageStrings>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    STAGE_HORNS(
        Vector3f(-52.16f, 67.44f, -51.44f),
        Quaternion(-0.01f, 0.99f, -0.09f, -0.07f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, StageHorns::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<StageHorns>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    PIZZICATO_STRINGS(
        Vector3f(-68.83f, 56.76f, -52.56f),
        Quaternion(-0.05f, 0.95f, -0.24f, -0.19f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, PizzicatoStrings::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<PizzicatoStrings>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    ACCORDION(
        Vector3f(-55.93f, 44.35f, -16.56f),
        Quaternion(-0.03f, 0.97f, -0.16f, -0.16f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Accordion::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Accordion>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    BANJO(
        Vector3f(32.83f, 62.14f, 46.33f),
        Quaternion(0.03f, 0.97f, -0.12f, 0.23f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Banjo::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Banjo>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    SHAMISEN(
        Vector3f(40.64f, 73.55f, 30.32f),
        Quaternion(0.03f, 0.97f, -0.11f, 0.24f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Shamisen::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Shamisen>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TIMPANI(
        Vector3f(43.85f, 55.59f, -38.73f),
        Quaternion(0.01f, 0.98f, -0.21f, 0.07f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Timpani::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Timpani>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    MELODIC_TOM(
        Vector3f(54.01f, 83.03f, -52.03f),
        Quaternion(0.01f, 0.98f, -0.19f, 0.04f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, MelodicTom::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<MelodicTom>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    SYNTH_DRUM(
        Vector3f(10.73f, 103.13f, -78.50f),
        Quaternion(0.06f, 0.90f, -0.13f, 0.42f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, SynthDrum::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<SynthDrum>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TAIKO_DRUM(
        Vector3f(19.72f, 99.17f, -121.31f),
        Quaternion(0.03f, 0.91f, -0.07f, 0.42f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, TaikoDrum::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<TaikoDrum>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TROMBONE(
        Vector3f(28.212189f, 86.88116f, 41.177956f),
        Quaternion(0.026606327f, 0.97332513f, -0.1550696f, 0.16698427f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Trombone::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Trombone>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    FLUTE(
        Vector3f(5.84f, 54.35f, 9.74f),
        Quaternion(-0.00f, 1.00f, -0.06f, -0.03f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Flute::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Flute>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    PICCOLO(
        Vector3f(5.84f, 60.25f, 9.74f),
        Quaternion(-0.00f, 1.00f, -0.06f, -0.03f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Piccolo::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Piccolo>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    RECORDER(
        Vector3f(-4.77f, 48.54f, 9.77f),
        Quaternion(0.01f, 0.99f, -0.07f, 0.09f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Recorder::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Recorder>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    HARMONICA(
        Vector3f(56.07f, 38.10f, -30.10f),
        Quaternion(0.10f, 0.86f, -0.18f, 0.47f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Harmonica::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Harmonica>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    PAN_FLUTE(
        Vector3f(58.05f, 37.40f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.16f, 0.18f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, PanFlute::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<PanFlute>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    WHISTLES(
        Vector3f(58.05f, 37.40f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.20f, 0.17f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Whistles::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Whistles>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    BLOWN_BOTTLE(
        Vector3f(58.05f, 31.78f, 0.82f),
        Quaternion(0.03f, 0.97f, -0.17f, 0.16f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, BlownBottle::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<BlownBottle>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    AGOGOS(
        Vector3f(55.97f, 32.62f, 11.38f),
        Quaternion(0.02f, 0.98f, -0.17f, 0.11f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Agogos::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Agogos>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    WOODBLOCKS(
        Vector3f(54.60f, 29.79f, 17.34f),
        Quaternion(0.01f, 0.99f, -0.13f, 0.11f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Woodblocks::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Woodblocks>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TUBA(
        Vector3f(-75.00f, 33.61f, 9.40f),
        Quaternion(-0.02f, 0.97f, -0.11f, -0.22f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Tuba::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Tuba>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    FRENCH_HORN(
        Vector3f(-82.14993f, 43.444687f, 30.638006f),
        Quaternion(-0.05368753f, 0.9447794f, -0.19924761f, -0.2545779f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, FrenchHorn::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<FrenchHorn>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TRUMPET(
        Vector3f(-0.17f, 61.50f, 30.30f),
        Quaternion(-0.01f, 0.93f, -0.03f, -0.37f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Trumpet::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Trumpet>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    OBOE(
        Vector3f(18.71f, 53.21f, 35.69f),
        Quaternion(-0.01f, 0.98f, -0.17f, -0.05f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Oboe::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Oboe>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    CLARINET(
        Vector3f(-13.75f, 52.54f, 37.02f),
        Quaternion(-0.01f, 0.99f, -0.15f, -0.04f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Clarinet::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Clarinet>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    STEEL_DRUMS(
        Vector3f(46.01f, 62.34f, -29.49f),
        Quaternion(0.02f, 0.97f, -0.18f, 0.13f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, SteelDrums::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<SteelDrums>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    FIDDLE(
        Vector3f(-7.67f, 79.05f, 19.95f),
        Quaternion(-0.01f, 0.98f, -0.18f, -0.04f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Fiddle::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Fiddle>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    OCARINA(
        Vector3f(36.71f, 54.71f, 33.44f),
        Quaternion(0.04f, 0.96f, -0.18f, 0.21f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Ocarina::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Ocarina>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    BAG_PIPE(
        Vector3f(-49.76239f, 33.13658f, 82.276375f),
        Quaternion(-0.0023348634f, 0.9805548f, -0.011687864f, -0.19588324f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, BagPipe::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<BagPipe>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    KALIMBA(
        Vector3f(20.016724f, 50.83171f, 53.29627f),
        Quaternion(-0.0011894251f, 0.9525223f, -0.30444378f, -0.0037213443f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, Kalimba::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<Kalimba>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    TINKLE_BELL(
        Vector3f(34.142365f, 50.831703f, 54.312134f),
        Quaternion(0.024830274f, 0.94041014f, -0.33174983f, 0.070386335f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, TinkleBell::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<TinkleBell>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

    BIRD_TWEET(
        Vector3f(126.65341f, 55.186848f, -5.279584f),
        Quaternion(0.027039362f, 0.9644452f, -0.1090079f, 0.2392313f),
        { time, instruments, context -> !context.configs.getType(SettingsConfiguration::class).isClassicCamera && visibleNowAndLater(instruments, BirdTweet::class.java, time, WAIT_TIME * 1.5) },
        { _, instruments, _ -> instruments.filterIsInstance<BirdTweet>().any { it.isVisible } },
        AutoCamPositionType.INSTRUMENT
    ),

}

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
    it.isVisible && it.calculateVisibility(time + buffer, future = true)
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