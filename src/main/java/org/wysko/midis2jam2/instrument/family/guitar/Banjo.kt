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
package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial.CullHint.Always
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/**
 * The Banjo.
 *
 * @constructor Creates a Banjo.
 *
 * @param context context to the main class
 * @param events the list of events for this Banjo
 */
class Banjo(context: Midis2jam2, events: List<MidiChannelSpecificEvent>) : FrettedInstrument(
    context,
    StandardFrettingEngine(4, 17, intArrayOf(48, 55, 62, 69)),
    events,
    FrettedInstrumentPositioning(
        13.93f,
        -19.54f, arrayOf(
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f),
            Vector3f(1f, 1f, 1f)
        ), floatArrayOf(-0.53f, -0.13f, 0.28f, 0.68f), floatArrayOf(-1.14f, -0.40f, 0.47f, 1.21f),
        FretHeightByTable.fromXml("Banjo")
    ),
    4,
    context.loadModel("Banjo.fbx", "BanjoSkin.png")
) {

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = Vector3f(7f, -2.43f, 0f).mult(updateInstrumentIndex(delta))
    }

    companion object {
        private val BASE_POSITION = Vector3f(54.6f, 48.7f, 2f)
    }

    init {
        /* Load strings */
        upperStrings = Array(4) {
            context.loadModel("BanjoString.fbx", "BassSkin.bmp").also {
                instrumentNode.attachChild(it)
            }
        }

        /* Position each string */
        val forward = 0
        upperStrings[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward.toFloat())
        upperStrings[0].localRotation = Quaternion().fromAngles(0f, 0f, rad(-1.03))
        upperStrings[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward.toFloat())
        upperStrings[1].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.47))
        upperStrings[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward.toFloat())
        upperStrings[2].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.33))
        upperStrings[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward.toFloat())
        upperStrings[3].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.92))

        /* Lower strings */
        lowerStrings = Array(4) {
            Array(5) { j: Int ->
                context.loadModel("BanjoStringBottom$j.fbx", "BassSkin.bmp").apply {
                    instrumentNode.attachChild(this)
                }
            }
        }

        /* Position lower strings */
        for (i in 0..4) {
            lowerStrings[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, forward.toFloat())
            lowerStrings[0][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-1.04))
        }
        for (i in 0..4) {
            lowerStrings[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, forward.toFloat())
            lowerStrings[1][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(-0.46))
        }
        for (i in 0..4) {
            lowerStrings[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, forward.toFloat())
            lowerStrings[2][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.33))
        }
        for (i in 0..4) {
            lowerStrings[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, forward.toFloat())
            lowerStrings[3][i].localRotation = Quaternion().fromAngles(0f, 0f, rad(0.91))
        }

        /* Hide all wobbly strings */
        for (i in 0..3) {
            for (j in 0..4) {
                lowerStrings[i][j].cullHint = Always
            }
        }

        /* Initialize note fingers */
        noteFingers = Array(4) {
            context.loadModel("BassNoteFinger.obj", "BanjoSkin.png").apply {
                instrumentNode.attachChild(this)
                this.cullHint = Always
            }
        }

        /* Position guitar */
        instrumentNode.apply {
            localTranslation = BASE_POSITION
            localRotation = Quaternion().fromAngles(rad(0.8), rad(-43.3), rad(-40.5))
        }
    }
}