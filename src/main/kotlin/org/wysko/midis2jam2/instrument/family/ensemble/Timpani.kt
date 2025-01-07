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
package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussive.OneDrumOctave
import org.wysko.midis2jam2.instrument.family.percussive.modulus
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

/**
 * The Timpani.
 */
class Timpani(context: Midis2jam2, eventList: List<MidiEvent>) : OneDrumOctave(context, eventList) {

    override fun adjustForMultipleInstances(delta: Duration) {
        root.rot = v3(0f, -27 + updateInstrumentIndex(delta) * -18, 0f)
    }

    override val strikers: Array<Striker> = Array(12) { i ->
        Striker(
            context = context,
            strikeEvents = eventList.modulus(i),
            stickModel = context.modelD("XylophoneMalletWhite.obj", "XylophoneBar.bmp")
        ).apply {
            setParent(recoilNode)
            offsetStick { it.loc = v3(0, 0, -5) }
            node.loc = v3(1.8 * (i - 5.5), 31, 15)
        }
    }

    init {
        with(recoilNode) {
            +context.modelR("TimpaniBody.obj", "HornSkin.bmp").apply {
                (this as Node)[0].material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp")
            }
            +context.modelD("TimpaniHead.obj", "TimpaniSkin.bmp")
        }
        placement.loc = v3(0, 0, -120)
    }
}
