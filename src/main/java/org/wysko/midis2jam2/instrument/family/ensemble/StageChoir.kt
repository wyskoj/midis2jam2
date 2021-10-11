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
package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.brass.BouncyTwelfth
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The choir. */
class StageChoir(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: ChoirType) :
    WrappedOctaveSustained(context, eventList, true) {

    override fun moveForMultiChannel(delta: Float) {
        val indexForMoving = updateInstrumentIndex(delta)
        twelfths.forEach {
            it as ChoirPeep
            if (indexForMoving >= 0) {
                it.highestLevel.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, 10f, -15f).mult(indexForMoving))
            } else {
                it.highestLevel.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, indexForMoving * 10f, indexForMoving * 10f))
            }
        }
    }

    /** A single choir peep. */
    inner class ChoirPeep(type: ChoirType) : BouncyTwelfth() {
        init {
            animNode.attachChild(context.loadModel("StageChoir.obj", type.textureFile))
        }
    }

    /** The type of choir peep. */
    enum class ChoirType(
        /** The texture file of the choir type. */
        val textureFile: String
    ) {
        /** Voice aahs. */
        CHOIR_AAHS("ChoirPeep.bmp"),

        /** Voice oohs. */
        VOICE_OOHS("ChoirPeepOoh.png"),

        /** Synth voice. */
        SYNTH_VOICE("ChoirPeepSynthVoice.png"),

        /** Voice synth. */
        VOICE_SYNTH("ChoirPeepVoiceSynth.png");
    }

    companion object {
        private val BASE_POSITION = Vector3f(0f, 29.5f, -152.65f)
    }

    init {
        twelfths = Array(12) { ChoirPeep(type) }
        val peepNodes = Array(12) { Node() }

        /* Load each peep */
        peepNodes.forEachIndexed { index, node ->
            node.attachChild(twelfths[index].highestLevel)
            node.localRotation = Quaternion().fromAngles(0f, rad(11.27 + index * -5.636), 0f)
            instrumentNode.attachChild(node)
        }

        twelfths.forEach { it.highestLevel.localTranslation = BASE_POSITION }
    }
}