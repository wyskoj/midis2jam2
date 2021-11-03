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
package org.wysko.midis2jam2.instrument.family.strings

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.guitar.FretHeightCalculator
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.instrument.family.guitar.StandardFrettingEngine
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad
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
        4, 48, openStringMidiNotes
    ),
    events,
    FrettedInstrumentPositioningWithZ(
        8.84f,
        -6.17f, arrayOf(
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f)
        ), floatArrayOf(-0.369f, -0.122f, 0.126f, 0.364f), floatArrayOf(-0.8f, -0.3f, 0.3f, 0.8f),
        object : FretHeightCalculator {
            override fun calculateScale(fret: Int): Float {
                return 1 - (0.0003041886 * fret.toDouble().pow(2.0) + -0.0312677 * fret + 1).toFloat()
            }
        }, floatArrayOf(-0.6f, -0.6f, -0.6f, -0.6f), floatArrayOf(
            0.47f,
            0.58f,
            0.58f,
            0.47f
        )
    ),
    4,
    body
) {

    /** The bow of this string instrument. */
    private val bow: Spatial

    /** The Bow node. */
    private val bowNode = Node()

    /** True if the bow is going left, false if the bow is going right. */
    var bowGoesLeft = false

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        animateBow(delta)
    }

    override fun handleStrings(time: Double, delta: Float): Boolean {
        val b = super.handleStrings(time, delta)
        if (b) {
            /* Reverse bow direction */
            bowGoesLeft = !bowGoesLeft
        }
        return b
    }

    /** Position upper strings. */
    private fun positionUpperStrings() {
        val forward = -0.6f
        upperStrings[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward)
        upperStrings[0].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(-1.63))
        upperStrings[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward)
        upperStrings[1].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(-0.685))
        upperStrings[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward)
        upperStrings[2].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(0.667))
        upperStrings[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward)
        upperStrings[3].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(1.69))
    }

    /** Load lower strings. */
    private fun loadLowerStrings() {
        // Lower strings
        lowerStrings = Array(4) {
            Array(5) { j: Int ->
                context.loadModel("ViolinStringPlayed$j.obj", "DoubleBassSkin.bmp").apply {
                    instrumentNode.attachChild(this)
                }
            }
        }

        // Position lower strings
        for (i in 0..4) {
            lowerStrings[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, 0.47f)
            lowerStrings[0][i].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(-1.61))
        }
        for (i in 0..4) {
            lowerStrings[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, 0.58f)
            lowerStrings[1][i].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(-0.663))
        }
        for (i in 0..4) {
            lowerStrings[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, 0.58f)
            lowerStrings[2][i].localRotation = Quaternion().fromAngles(rad(-4.6), 0f, rad(0.647))
        }
        for (i in 0..4) {
            lowerStrings[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, 0.47f)
            lowerStrings[3][i].localRotation = Quaternion().fromAngles(rad(-4.0), 0f, rad(1.65))
        }

        // Hide all wobbly strings
        for (i in 0..3) {
            for (j in 0..4) {
                lowerStrings[i][j].cullHint = Spatial.CullHint.Always
            }
        }
    }

    /** Loads the note fingers. */
    private fun loadNoteFingers() {
        // Initialize note fingers
        noteFingers = Array(4) {
            context.loadModel("BassNoteFinger.obj", "BassSkin.bmp").apply {
                instrumentNode.attachChild(this)
                this.cullHint = Spatial.CullHint.Always
                setLocalScale(0.75f)
            }
        }
    }

    /**
     * Animates the movement of the bow.
     *
     * @param delta time since the last frame
     */
    private fun animateBow(delta: Float) {
        if (currentNotePeriods.isNotEmpty()) {
            bowNode.setLocalTranslation(0f, -4f, 0.5f)
            if (bowGoesLeft) {
                bow.move(-3 * delta, 0f, 0f)
            } else {
                bow.move(3 * delta, 0f, 0f)
            }
            if (bow.localTranslation.x > 7) {
                bow.setLocalTranslation(7f, 0f, 0f)
                bowGoesLeft = true
            }
            if (bow.localTranslation.x < -7) {
                bow.setLocalTranslation(-7f, 0f, 0f)
                bowGoesLeft = false
            }
        } else {
            val pos = bowNode.localTranslation
            if (pos.z < 1) {
                bowNode.localTranslation = pos.setZ(pos.z + 1 * delta)
            }
        }
    }

    init {
        body.setLocalTranslation(0f, 0f, -1.2f)
        instrumentNode.attachChild(body)
        bow = context.loadModel("ViolinBow.obj", "ViolinSkin.bmp")
        bowNode.attachChild(bow)
        instrumentNode.attachChild(bowNode)
        bowNode.localScale = bowScale
        bowNode.setLocalTranslation(0f, -4f, 1f)
        bowNode.localRotation = Quaternion().fromAngles(rad(180.0), rad(180.0), rad(bowRotation))
        if (!showBow) {
            bowNode.cullHint = Spatial.CullHint.Always
        }
        upperStrings = Array(4) {
            context.loadModel("ViolinString.obj", "ViolinSkin.bmp").apply {
                instrumentNode.attachChild(this)
            }
        }
        positionUpperStrings()
        loadLowerStrings()
        loadNoteFingers()
    }
}