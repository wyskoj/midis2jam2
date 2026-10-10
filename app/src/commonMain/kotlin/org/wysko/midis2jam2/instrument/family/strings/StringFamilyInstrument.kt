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
package org.wysko.midis2jam2.instrument.family.strings

import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.BeatGrid
import org.wysko.midis2jam2.instrument.family.guitar.FretHeightCalculator
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.instrument.family.guitar.FrettingPlan
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingProfile
import org.wysko.midis2jam2.instrument.family.strings.bowing.*
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.STRING_GLOW
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * The type String family instrument, including the violin, viola, cello, and double bass.
 *
 * @see Violin
 * @see Viola
 * @see Cello
 * @see AcousticBass
 */
abstract class StringFamilyInstrument protected constructor(
    /** Context to midis2jam2. */
    context: PerformanceManager,
    events: List<MidiEvent>,
    showBow: Boolean,
    bowRotation: Double,
    bowScale: Vector3f,
    profile: FrettingProfile,
    body: Spatial,
    bowingProfile: BowingProfile = BowingProfile.Small,
) : FrettedInstrument(
    context,
    events,
    FrettingPlan.create(context, events, profile),
    FrettedInstrumentPositioningWithZ(
        8.84f,
        -6.17f,
        arrayOf(
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
        ),
        floatArrayOf(-0.369f, -0.122f, 0.126f, 0.364f),
        BRIDGE_X.map { it.toFloat() }.toFloatArray(),
        object : FretHeightCalculator {
            override fun calculateScale(fret: Int): Float {
                return 1 - (0.0003041886 * fret.toDouble().pow(2.0) + -0.0312677 * fret + 1).toFloat()
            }
        },
        floatArrayOf(-0.6f, -0.6f, -0.6f, -0.6f),
        BRIDGE_Z.map { it.toFloat() }.toFloatArray(),
    ),
    4,
    body to Materials.Diffuse.GuitarSkin,
) {
    override val upperStrings: Array<Spatial> =
        Array(4) {
            context.model(Models.Strings.Violin.String).apply {
                geometry.attachChild(this)
            }
        }.apply {
            val forward = -0.6f
            this[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward)
            this[0].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(-1.63))
            this[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward)
            this[1].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(-0.685))
            this[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward)
            this[2].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(0.667))
            this[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward)
            this[3].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(1.69))
        }

    override val lowerStrings: List<List<Spatial>> =
        List(4) {
            Models.Strings.Violin.StringPlayed.map { frame ->
                context.model(frame).apply {
                    geometry.attachChild(this)
                    (this as Geometry).material.setColor("GlowColor", STRING_GLOW)
                }
            }
        }.apply {
            // Position lower strings
            for (i in 0..4) {
                this[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, 0.47f)
                this[0][i].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(-1.61))
            }
            for (i in 0..4) {
                this[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, 0.58f)
                this[1][i].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(-0.663))
            }
            for (i in 0..4) {
                this[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, 0.58f)
                this[2][i].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(0.647))
            }
            for (i in 0..4) {
                this[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, 0.47f)
                this[3][i].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(1.65))
            }

            // Hide them all
            this.flatMap { it.asIterable() }.forEach { it.cullHint = Spatial.CullHint.Always }
        }

    /** The Bow node. */
    internal val bowNode =
        Node().apply {
            localScale = bowScale
            setLocalTranslation(0f, -4f, 1f)
            localRotation = Quaternion().fromAngles(rad(180.0), rad(180.0), rad(bowRotation))
            cullHint = showBow.ch
        }.also {
            geometry.attachChild(it)
        }

    /** The bow of this string instrument. */
    private val bow: Spatial =
        context.model(Models.Strings.Violin.Bow).apply {
            bowNode.attachChild(this)
        }

    /** How the part is bowed, decided once from the notes and the strings the fretting engine put them on. */
    private val bowMotion: BowMotion = run {
        val openStrings = fretting.tuning.openStrings
        val notes = timedArcs.map { arc ->
            val string = fretting.positions[arc]?.string
                ?: openStrings.indexOfLast { it <= arc.note.toInt() }.coerceAtLeast(0)
            BowNote(arc.startTime.toDouble(DurationUnit.SECONDS), arc.endTime.toDouble(DurationUnit.SECONDS), string, arc.velocity.toInt())
        }
        val grid = BeatGrid.from(context.sequence)
        // A note starting on a bar line is a strong beat, which a player would take down-bow.
        val onBarLine = { time: Double -> grid.nearestBarLine(time, STRONG_BEAT_TOLERANCE) != null }
        BowMotion(BowingPlanner.plan(notes, bowingProfile, onBarLine), StringContactMap(BRIDGE_X, BRIDGE_Z))
    }

    /** The bow's resting orientation, before it's tilted onto a string. */
    private val bowBaseRotation = Quaternion().fromAngles(rad(180.0), rad(180.0), rad(bowRotation))

    private var lastPose: BowPose = bowMotion.poseAt(0.0)

    init {
        body.setLocalTranslation(0f, 0f, -1.2f)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        animateBow(time)
    }

    private fun animateBow(time: Duration) {
        if (bowNode.cullHint == Spatial.CullHint.Always) return
        val pose = bowMotion.poseAt(time.toDouble(DurationUnit.SECONDS)).also { lastPose = it }

        // Slide the bow so the point at the strings runs from the frog (+x in the model) to the tip.
        bow.loc = v3(BOW_CONTACT_RANGE * (2 * pose.position - 1), 0, 0)

        // Tilt about the strings' direction so the hair lies along the arch where the sounding strings are.
        val tilt = Quaternion().fromAngles(0f, -atan(pose.line.slope).toFloat(), 0f)
        bowNode.localRotation = tilt.mult(bowBaseRotation)

        val contactZ = BOW_DOWN_Z + (pose.line.z0 - BRIDGE_REFERENCE_Z).toFloat() - BOW_PRESSURE_DIP * pose.pressure.toFloat()
        bowNode.loc = v3(0, -4, contactZ + (BOW_RAISED_Z - BOW_DOWN_Z) * pose.lift.toFloat())
    }

    override fun readoutExtra(time: Double): String? {
        val plan = bowMotion.plan
        val stroke = plan.strokeAt(time) ?: plan.nextAfter(time)
        val pose = bowMotion.poseAt(time)
        return buildString {
            append("bow ")
            if (stroke == null) {
                append("no strokes")
            } else {
                append(if (plan.strokeAt(time) == null) "next " else "").append(stroke.direction.name.lowercase())
                append("  strings ").append(stroke.stringsAt(time).sorted().joinToString("+"))
                append("  ").append(if (stroke.events.size > 1) "slur x${stroke.events.size}" else "single")
                if (stroke.retake) append("  retake")
            }
            append("\npos ").append(round2(pose.position))
            append("  lift ").append(round2(pose.lift))
        }
    }

    override fun toString(): String =
        super.toString() +
                formatProperty("bowPosition", lastPose.position) +
                formatProperty("bowLift", lastPose.lift) +
                formatProperty("bowSlope", lastPose.line.slope)
}

/** Where the strings cross the bridge, sideways and in height. Shared by the string models and the bow. */
private val BRIDGE_X = doubleArrayOf(-0.8, -0.3, 0.3, 0.8)
private val BRIDGE_Z = doubleArrayOf(0.47, 0.58, 0.58, 0.47)

/** How far the bow slides in either direction from its centre, in model units. */
private const val BOW_CONTACT_RANGE = 6.75f

/** The bow's height with the hair on the strings, when the bridge's height is [BRIDGE_REFERENCE_Z]. */
private const val BOW_DOWN_Z = 0.5f
private const val BOW_RAISED_Z = 2.0f
private const val BRIDGE_REFERENCE_Z = 0.55
private const val BOW_PRESSURE_DIP = 0.05f

/** How close to a bar line, in seconds, a note must start to count as being on the strong beat. */
private const val STRONG_BEAT_TOLERANCE = 0.05

private fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0
