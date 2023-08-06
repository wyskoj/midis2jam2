/*
 * Copyright (C) 2023 Jacob Wysko
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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.guitar.FretHeightCalculator
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.instrument.family.guitar.StandardFrettingEngine
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriodGroup
import org.wysko.midis2jam2.midi.contiguousGroups
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.cullHint
import org.wysko.midis2jam2.world.STRING_GLOW
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

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
    context: Midis2jam2,

    /* The list of events for this instrument. */
    events: List<MidiChannelSpecificEvent>,

    /* True if the bow should be shown, false otherwise. */
    showBow: Boolean,

    /* The rotation of the bow. */
    bowRotation: Double,

    /* How large to display the bow. */
    bowScale: Vector3f,

    /* The MIDI note value of each string played open. */
    openStringMidiNotes: IntArray,

    /* The body of the instrument. */
    body: Spatial
) : FrettedInstrument(
    context,
    StandardFrettingEngine(
        4,
        48,
        openStringMidiNotes
    ),
    events,
    FrettedInstrumentPositioningWithZ(
        8.84f,
        -6.17f,
        arrayOf(
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f)
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
            0.47f
        )
    ),
    4,
    body,
    "GuitarSkin.bmp"
) {

    override val upperStrings: Array<Spatial> = Array(4) {
        context.loadModel("ViolinString.obj", "ViolinSkin.bmp").apply {
            instrumentNode.attachChild(this)
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

    override val lowerStrings: Array<Array<Spatial>> = Array(4) {
        Array(5) { j: Int ->
            context.loadModel("ViolinStringPlayed$j.obj", "DoubleBassSkin.bmp").apply {
                instrumentNode.attachChild(this)
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
    private val bowNode = Node().apply {
        localScale = bowScale
        setLocalTranslation(0f, -4f, 1f)
        localRotation = Quaternion().fromAngles(rad(180.0), rad(180.0), rad(bowRotation))
        cullHint = showBow.cullHint()
    }.also {
        instrumentNode.attachChild(it)
    }

    /** The bow of this string instrument. */
    private val bow: Spatial = context.loadModel("ViolinBow.obj", "ViolinSkin.bmp").apply {
        bowNode.attachChild(this)
    }

    /** The current intensity of the bow animation. A larger intensity means the bows will travel further. */
    private var intensity = 3.0

    private val groups: MutableList<NotePeriodGroup> = notePeriods.contiguousGroups().toMutableList()
    private var currentGroup: NotePeriodGroup? = null

    init {
        body.setLocalTranslation(0f, 0f, -1.2f)
    }

    /** True if the bow is going left, false if the bow is going right. */
    private var bowGoesLeft = false

    override fun tick(time: Double, delta: Float) {
        currentGroup?.let {
            if (it.endTime() <= time) currentGroup = null
        }

        groups.takeWhile { it.startTime() <= time }.let {
            if (it.isNotEmpty()) {
                groups.removeAll(it.toSet())
                currentGroup = it.last()
                bowGoesLeft = !bowGoesLeft
            }
        }

        super.tick(time, delta)
        animateBow(time, delta)
    }

    /**
     * Animates the movement of the bow.
     *
     * @param delta time since the last frame
     */
    private fun animateBow(time: Double, delta: Float) {
        if (currentNotePeriods.isNotEmpty()) {
            bowNode.setLocalTranslation(0f, -4f, 0.5f)
            val p = currentGroup ?: return
            var progress = ((time - p.startTime()) / p.duration())
            if (bowGoesLeft) progress = 1 - progress
            progress = progress.smooth(
                when {
                    p.duration() < 0.5 -> 0
                    p.duration() < 1 -> 1
                    p.duration() < 2 -> 2
                    p.duration() < 3 -> 3
                    else -> 4
                }
            )

            val targetIntensity = when {
                p.duration() < 0.2 -> 2
                p.duration() < 0.5 -> 3
                p.duration() < 0.75 -> 4
                p.duration() < 1 -> 5
                else -> 6
            }

            intensity += (targetIntensity - intensity) * 0.1

            bow.setLocalTranslation(((progress * intensity * 2) - intensity).toFloat(), 0f, 0f)
        } else {
            notePeriodCollector.peek()?.let {
                if (it.startTime - time < 1) {
                    with(bowNode.localTranslation) {
                        if (this.z > 0.5) {
                            bowNode.localTranslation = this.setZ((this.z - 2 * delta).coerceAtLeast(0.5f))
                        }
                    }
                } else {
                    with(bowNode.localTranslation) {
                        if (this.z < 2) {
                            bowNode.localTranslation = this.setZ((this.z + 2 * delta).coerceAtMost(2f))
                        }
                    }
                }
            }
        }
    }

    override fun toString(): String {
        return super.toString() + buildString {
            append(debugProperty("intensity", intensity.toFloat()))
            append(debugProperty("np", currentGroup?.let { "${it.startTime()}..${it.endTime()}" } ?: ""))
        }
    }
}

private fun Double.smooth(smoothness: Int): Double {
    return when (smoothness) {
        0 -> this // Linear
        1 -> (this + (-(cos(PI * this) - 1) / 2)) / 2.0
        2 -> -(cos(PI * this) - 1) / 2 // sine
        3 -> if (this < 0.5) 2 * this.pow(2) else 1 - (-2 * this + 2).pow(2) / 2
        else -> if (this < 0.5) 4 * this.pow(3) else 1 - (-2 * this + 2).pow(3) / 2
    }
}
