/*
 * Copyright (C) 2024 Jacob Wysko
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

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.instrument.algorithmic.PitchBendModulationController
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.notePeriodsModulus
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior.OUTWARDS
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffTexture.NORMAL
import org.wysko.midis2jam2.util.NumberSmoother
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.material
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.scale
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelD

/**
 * The Pan Flute.
 */
class PanFlute(context: Midis2jam2, eventList: List<MidiChannelEvent>, skin: PipeSkin) :
    DivisiveSustainedInstrument(context, eventList) {

    override val animators: List<PitchClassAnimator> = List(12) { index ->
        val i = 11 - index
        PanFlutePipe(skin, eventList.notePeriodsModulus(context, i)).apply {
            root.loc = v3(-4.248 * 0.9, -3.5 + 0.38 * i, -11.151 * 0.9)
            root.rot = v3(0, 180, 0)
            geometry.scale = v3(1, 1 + (13 - i) * 0.05, 1)
            puffer.root.loc = v3(0, 11.75 - 0.38 * i, 0)
        }.also {
            node {
                +it.root
                rot = v3(0, 7.272 * i + 75, 0)
            }.also { geometry += it }
        }
    }

    private val pitchBendModulationController = PitchBendModulationController(context, eventList)
    private var bend = 0f

    init {
        placement.loc = v3(75f, 22f, -35f)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        bend = pitchBendModulationController.tick(time, delta)
    }

    override fun adjustForMultipleInstances(delta: Float) {
        geometry.rot = v3(0, 80 / 11.0 * 12 * updateInstrumentIndex(delta), 0)
        root.loc = v3(0, index * 4.6, 0)
    }

    /**
     * Represents a skin the Pan Flute can have.
     */
    enum class PipeSkin(
        internal val textureFile: String,
        internal val reflective: Boolean,
    ) {
        /** Gold pipe skin. */
        GOLD("HornSkin.bmp", true),

        /** Wood pipe skin. */
        WOOD("Wood.bmp", false),
    }

    /** Each of the pipes in the pan flute, calliope, etc. */
    private inner class PanFlutePipe(skin: PipeSkin, notePeriodsModulus: List<NotePeriod>) :
        PitchClassAnimator(context, notePeriodsModulus) {

        val puffer = SteamPuffer(context, NORMAL, 1.0, OUTWARDS)
        val bendCtrl = NumberSmoother(0f, 10.0)

        init {
            with(geometry) {
                +context.modelD("PanPipe.obj", skin.textureFile).apply {
                    if (skin.reflective) {
                        material = context.assetLoader.reflectiveMaterial(skin.textureFile)
                    }
                }
            }
            with(animation) {
                +puffer.root.also {
                    it.rot = v3(0, 0, 90)
                }
            }
        }

        override fun tick(time: Double, delta: Float) {
            super.tick(time, delta)
            puffer.tick(delta, playing)
            animation.loc = v3(0, bendCtrl.tick(delta) { if (playing) bend else 0f }, 0)
        }
    }
}
