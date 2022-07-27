/*
 * Copyright (C) 2022 Jacob Wysko
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

import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.WrappedOctaveSustained
import org.wysko.midis2jam2.instrument.family.brass.BouncyTwelfth
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

private val BASE_POSITION = Vector3f(0f, 29.5f, -152.65f)

/** The choir. */
class StageChoir(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, type: ChoirType) :
    WrappedOctaveSustained(context, eventList, true) {

    override val twelfths: Array<TwelfthOfOctave> = Array(12) {
        if (type == ChoirType.HALO_SYNTH) {
            ChoirPeepHalo()
        } else {
            ChoirPeep(type)
        }.apply {
            highestLevel.localTranslation = BASE_POSITION
        }
    }

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
    open inner class ChoirPeep(type: ChoirType) : BouncyTwelfth() {
        open val model = context.loadModel("StageChoir.obj", type.textureFile).also {
            animNode.attachChild(it)
        }
    }

    inner class ChoirPeepHalo : ChoirPeep(ChoirType.HALO_SYNTH) {
        override val model = context.loadModel("StageChoirHalo.obj", "ChoirHalo.png").also {
            /* Halo must be separate material for glow effect to work */
            (it as Node).getChild(0).apply {
                setMaterial(context.unshadedMaterial("ChoirHalo.png"))
                shadowMode = RenderQueue.ShadowMode.Off
            }
            animNode.attachChild(it)
        }

        override fun tick(delta: Float) {
            super.tick(delta)

            ((model as Node).getChild(0) as Geometry).material.setColor(
                "GlowColor", if (playing) {
                    ColorRGBA.Yellow
                } else {
                    ColorRGBA.Black
                }
            )
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
        VOICE_SYNTH("ChoirPeepVoiceSynth.png"),

        /** Halo synth. */
        HALO_SYNTH("ChoirHalo.png"),

        /** Goblin synth. */
        GOBLIN_SYNTH("ChoirPeepGoblin.png"),
    }

    init {
        Array(12) {
            Node().apply {
                attachChild(twelfths[it].highestLevel)
                localRotation = Quaternion().fromAngles(0f, rad(11.27 + it * -5.636), 0f)
                instrumentNode.attachChild(this)
            }
        }
    }
}