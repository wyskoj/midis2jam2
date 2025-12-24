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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcGroupCollector
import org.wysko.midis2jam2.instrument.family.guitar.FretHeightCalculator
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.instrument.family.guitar.StandardFrettingEngine
import org.wysko.midis2jam2.midi.contiguousGroups
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.STRING_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
    openStringMidiNotes: IntArray,
    body: Spatial,
) : FrettedInstrument(
    context,
    events,
    StandardFrettingEngine(4, 24, openStringMidiNotes),
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
        floatArrayOf(-0.8f, -0.3f, 0.3f, 0.8f),
        object : FretHeightCalculator {
            override fun calculateScale(fret: Int): Float {
                return 1 - (0.0003041886 * fret.toDouble().pow(2.0) + -0.0312677 * fret + 1).toFloat()
            }
        },
        floatArrayOf(-0.6f, -0.6f, -0.6f, -0.6f),
        floatArrayOf(
            0.47f,
            0.58f,
            0.58f,
            0.47f,
        ),
    ),
    4,
    body to
    "GuitarSkin.bmp",
) {
    override val upperStrings: Array<Spatial> =
        Array(4) {
            context.modelD("ViolinString.obj", "ViolinSkin.bmp").apply {
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
            List(5) { j: Int ->
                context.modelD("ViolinStringPlayed$j.obj", "DoubleBassSkin.bmp").apply {
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
    private val bowNode =
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
        context.modelD("ViolinBow.obj", "ViolinSkin.bmp").apply {
            bowNode.attachChild(this)
        }

    private val groupCollector = TimedArcGroupCollector(context, timedArcs.contiguousGroups())

    init {
        body.setLocalTranslation(0f, 0f, -1.2f)
    }

    private var bowGoesLeft = false

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        groupCollector.advance(time)?.let {
            bowGoesLeft = !bowGoesLeft
        }
        animateBow(time, delta)
    }

    private var isBowDown = false
    private val bowRaise = NumberSmoother(2f, 7.0)
    private val dragIntensity = NumberSmoother(1f, 10.0)

    private fun animateBow(time: Duration, delta: Duration) {
        val progress = groupCollector.currentTimedArcGroup?.calculateProgress(time) ?: (1.0)

        bow.loc = v3(MAX_BOW_TRANSLATION, 0, 0) * bowGoesLeft.sign * (progress - 0.5) * dragIntensity.value

        dragIntensity.tick(delta) {
            if (groupCollector.currentTimedArcGroup == null) {
                groupCollector.peek()?.let {
                    if (it.startTime - time < 1.seconds) durationToIntensity(it.duration) else null
                } ?: dragIntensity.value
            } else {
                durationToIntensity(groupCollector.currentTimedArcGroup!!.duration)
            }
        }

        // Move bow up and down
        collector.peek()?.let {
            isBowDown = when {
                // Playing?
                groupCollector.currentTimedArcGroup != null -> true

                // Not playing, but about to play
                !isBowDown && it.startTime - time <= 0.5.seconds -> true

                // Just played, about to play again soon
                isBowDown && it.startTime - time < 5.seconds -> true

                // Not playing
                else -> false
            }
        }

        if (isBowDown) {
            bowNode.loc = v3(0, -4, bowRaise.tick(delta) { 0.5f })
        } else {
            bowNode.loc = v3(0, -4, bowRaise.tick(delta) { 2.0f })
        }
    }

    override fun toString(): String =
        super.toString() +
                formatProperty("intensity", dragIntensity.value) +
                formatProperty("bowGoesLeft", bowGoesLeft) +
                formatProperty("group", groupCollector.currentTimedArcGroup.toString()) +
                formatProperty("isBowDown", isBowDown)
}

private fun durationToIntensity(duration: Duration): Float = when {
    duration > 0.5.seconds -> 1f
    else -> duration.toDouble(DurationUnit.SECONDS).toFloat() * 2f
}.coerceAtLeast(0.2f)

private const val MAX_BOW_TRANSLATION = 10
