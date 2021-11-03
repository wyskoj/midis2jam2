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
package org.wysko.midis2jam2.instrument.family.pipe

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.util.Utils.rad

/** The Pan flute. */
class PanFlute(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, skin: PipeSkin) :
    WrappedOctaveSustained(context, eventList, false) {

    /** The Pipe nodes. */
    private val pipeNodes = Array(12) { Node() }

    override fun moveForMultiChannel(delta: Float) {
        val index = updateInstrumentIndex(delta)
        instrumentNode.localRotation =
            Quaternion().fromAngles(0f, rad((80 / 11f * 12 * index).toDouble()), 0f)
        offsetNode.setLocalTranslation(0f, index * 4.6f, 0f)
    }

    enum class PipeSkin(val textureFile: String, val reflective: Boolean) {
        /** Gold pipe skin. */
        GOLD("HornSkin.bmp", true),

        /** Wood pipe skin. */
        WOOD("Wood.bmp", false);
    }

    /** Each of the pipes in the pan flute, calliope, etc. */
    inner class PanFlutePipe(skin: PipeSkin) : TwelfthOfOctave() {

        /** The geometry of this pipe. */
        val pipe: Spatial = context.loadModel("PanPipe.obj", skin.textureFile)

        /** The steam puffer for this pipe. */
        val puffer: SteamPuffer

        override fun play(duration: Double) {
            playing = true
            progress = 0.0
            this.duration = duration
        }

        override fun tick(delta: Float) {
            if (progress >= 1) {
                playing = false
                progress = 0.0
            }
            if (playing) {
                progress += delta / duration
            }
            puffer.tick(delta, playing)
        }

        init {
            if (skin.reflective) {
                pipe.setMaterial(context.reflectiveMaterial("/Assets/" + skin.textureFile))
            }
            this.highestLevel.attachChild(pipe)
            puffer = SteamPuffer(context, SteamPuffer.SteamPuffType.NORMAL, 1.0, SteamPuffer.PuffBehavior.OUTWARDS)
            this.highestLevel.attachChild(puffer.steamPuffNode)
            puffer.steamPuffNode.localRotation = Quaternion().fromAngles(0f, 0f, rad(90.0))
        }
    }

    init {
        twelfths = Array(12) { index ->
            val i = 11 - index
            PanFlutePipe(skin).apply {
                highestLevel.setLocalTranslation(-4.248f * 0.9f, -3.5f + 0.38f * i, -11.151f * 0.9f)
                highestLevel.localRotation = Quaternion().fromAngles(0f, rad(180.0), 0f)
                pipe.setLocalScale(1f, 1 + (13 - i) * 0.05f, 1f)
                puffer.steamPuffNode.setLocalTranslation(0f, 11.75f - 0.38f * i, 0f)
            }.also {
                pipeNodes[i].localRotation = Quaternion().fromAngles(0f, rad(7.272 * i + 75), 0f)
                pipeNodes[i].attachChild(it.highestLevel)
            }
        }
        pipeNodes.forEach { instrumentNode.attachChild(it) }
        instrumentNode.setLocalTranslation(75f, 22f, -35f)
    }
}