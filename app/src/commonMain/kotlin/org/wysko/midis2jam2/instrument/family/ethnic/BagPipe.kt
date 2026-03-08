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

package org.wysko.midis2jam2.instrument.family.ethnic

import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.Behavior.Outwards
import org.wysko.midis2jam2.particle.SteamPuffer.Texture.Normal
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.material
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/**
 * The bag pipes.
 */
class BagPipe(
    context: PerformanceManager,
    events: List<MidiEvent>,
) : SustainedInstrument(context, events),
    MultipleInstancesLinearAdjustment {
    override val multipleInstancesDirection: Vector3f = v3(0, 30, 0)

    private val fingeringManager = HandPositionFingeringManager.from(this::class)

    private val leftHands =
        List(3) { context.modelD("BagPipeLeftHand${it + 1}.obj", "hands.bmp") }
            .onEachIndexed { i, hand -> geometry += hand.also { it.cullHint = (i == 0).ch } }

    private val rightHands =
        List(5) { context.modelD("BagPipeRightHand${it + 1}.obj", "hands.bmp") }
            .onEachIndexed { i, hand -> geometry += hand.also { it.cullHint = (i == 0).ch } }

    private val puffers =
        List(3) { SteamPuffer(context, Normal, 1.0, Outwards, Axis.Y) }.apply {
            forEach { geometry += it.root }
            with(this[0].root) {
                loc = v3(2.14, 11.68, -3.84)
                rot = v3(-5, 0, -17)
            }
            with(this[1].root) {
                loc = v3(-0.75, 3.17, 3.40)
                rot = v3(13, 0, -15)
            }
            with(this[2].root) {
                loc = v3(-0.75, 0.84, 8.67)
                rot = v3(32, 0, -12)
            }
        }

    init {
        with(geometry) {
            +context.modelD("BagPipe.obj", "BagPipeSkin.png").also {
                (it as Node).children.first().material = context.assetLoader.reflectiveMaterial("HornSkinGrey.bmp")
            }
        }
        with(placement) {
            loc = v3(-70, 40, 30)
            rot = v3(0, 80, 0)
        }
    }

    private fun removeSharpsAndFlats(note: Byte): Byte =
        with(note % 12) {
            when (this) {
                1, 3, 6, 8, 10 -> this - 1
                else -> this
            }
        }.toByte()

    private fun updateHandVisibility(givenHands: HandPositionFingeringManager.Hands) =
        with(givenHands) {
            leftHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == left).ch }
            rightHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == right).ch }
        }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        collector.advance(time).newlyRemovedTimedArcs.sortedBy { it.note }.forEach {
            val midiNote = removeSharpsAndFlats(it.note)
            fingeringManager.fingering(midiNote)?.let { hands ->
                updateHandVisibility(hands)
            }
        }
        puffers.forEach { it.tick(delta, collector.currentTimedArcs.isNotEmpty()) }
        isVisible = calculateVisibility(time)
        adjustForMultipleInstances(delta)
    }
}
