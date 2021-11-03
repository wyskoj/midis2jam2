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
 * The Stage Horns.
 *
 * Stage Horns animate no more special than any other [WrappedOctaveSustained] instrument.
 */
class StageHorns(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: StageHornsType) :
    WrappedOctaveSustained(context, eventList, false) {

    override fun moveForMultiChannel(delta: Float) {
        /* Get and update instrument index */
        val index = updateInstrumentIndex(delta)

        /* For each individual horn */
        twelfths.forEach {
            it as StageHornNote
            it.highestLevel.localTranslation = if (index >= 0) {
                Vector3f(BASE_POSITION).add(Vector3f(0f, 3f, -5f).mult(index))
            } else {
                Vector3f(BASE_POSITION).add(Vector3f(0f, 3f, 5f).mult(index))
            }
        }
    }

    /**
     * A single Stage Horn.
     *
     * It animates no more special than any other [BouncyTwelfth].
     */
    inner class StageHornNote(type: StageHornsType) : BouncyTwelfth() {
        init {
            animNode.attachChild(context.loadModel("StageHorn.obj", type.texture, MatType.REFLECTIVE, 0.9f))
        }
    }

    /** A type of Stage Horn. */
    enum class StageHornsType(
        /** The texture file of this Stage Horn type. */
        val texture: String
    ) {
        /** Brass section stage horns type. */
        BRASS_SECTION("HornSkin.bmp"),

        /** Synth brass 1 stage horns type. */
        SYNTH_BRASS_1("HornSkinGrey.bmp"),

        /** Synth brass 2 stage horns type. */
        SYNTH_BRASS_2("HornSkinCopper.png")
    }

    companion object {
        /** The base position of a Stage Horn. */
        private val BASE_POSITION = Vector3f(0f, 29.5f, -152.65f)
    }

    init {
        twelfths = Array(12) { StageHornNote(type) }
        val hornNodes = Array(12) { Node() }

        /* For each note */
        for (i in 0..11) {

            /* Attach model to node and rotate from center */
            hornNodes[i].run {
                attachChild(twelfths[i].highestLevel)
                localRotation = Quaternion().fromAngles(0f, rad(16 + i * 1.5), 0f)
            }

            twelfths[i].highestLevel.localTranslation = BASE_POSITION
            instrumentNode.attachChild(hornNodes[i])
        }
    }
}