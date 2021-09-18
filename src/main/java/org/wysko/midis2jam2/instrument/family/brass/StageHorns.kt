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
package org.wysko.midis2jam2.instrument.family.brass

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/**
 * The stage horns are positioned back and to the left. There are 12 of them for each note in the octave. Stage horns
 * are bouncy.
 *
 * @see BouncyTwelfth
 */
class StageHorns(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: StageHornsType) :
    WrappedOctaveSustained(context, eventList, false) {

    override fun moveForMultiChannel(delta: Float) {
        /* For each individual horn */
        for (twelfth in twelfths) {
            val horn = twelfth as StageHornNote
            /* Move each horn backwards along the axis that runs
            through the center of the stage and the base position */
            horn.highestLevel.localTranslation =
                Vector3f(BASE_POSITION).add(Vector3f(0f, 0f, -5f).mult(indexForMoving(delta)))
        }
    }

    /**
     * A single horn.
     */
    inner class StageHornNote(type: StageHornsType) : BouncyTwelfth() {
        init {
            animNode.attachChild(context.loadModel("StageHorn.obj", type.texture, MatType.REFLECTIVE, 0.9f))
        }
    }

    enum class StageHornsType(val texture: String) {
        /**
         * Brass section stage horns type.
         */
        BRASS_SECTION("HornSkin.bmp"),

        /**
         * Synth brass 1 stage horns type.
         */
        SYNTH_BRASS_1("HornSkinGrey.bmp"),

        /**
         * Synth brass 2 stage horns type.
         */
        SYNTH_BRASS_2("HornSkinCopper.png");
    }

    companion object {
        /**
         * The base position of a horn.
         */
        private val BASE_POSITION = Vector3f(0f, 29.5f, -152.65f)
    }

    init {
        twelfths = Array(12) { StageHornNote(type) }
        val hornNodes = Array(12) { Node() }

        /* For each note */
        for (i in 0..11) {

            /* Attach model to node and rotate from center */
            hornNodes[i].run {
                attachChild(twelfths[i]!!.highestLevel)
                localRotation = Quaternion().fromAngles(0f, rad(16 + i * 1.5), 0f)
            }

            twelfths[i]!!.highestLevel.localTranslation = BASE_POSITION
            instrumentNode.attachChild(hornNodes[i])
        }
    }
}