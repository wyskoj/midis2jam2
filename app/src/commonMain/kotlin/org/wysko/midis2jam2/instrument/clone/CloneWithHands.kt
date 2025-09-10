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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands
import org.wysko.midis2jam2.instrument.family.pipe.InstrumentWithHands
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.world.Axis
import kotlin.time.Duration

/**
 * Some instruments visualize notes by showing hands in different playing positions. To do this, a separate 3D model for
 * each hand in each "shape" is created; then they are seamlessly swapped out during playback to give the illusion that
 * the hands are moving.
 */
abstract class CloneWithHands protected constructor(parent: InstrumentWithHands, rotationFactor: Float) :
    Clone(parent, rotationFactor, Axis.X) {

    private val leftHandNode = Node()
    private val rightHandNode = Node()
    private var hands: Hands = Hands(0, 0)

    /** The left hands. */
    protected abstract val leftHands: List<Spatial>

    /** The right hands. */
    protected abstract val rightHands: List<Spatial>

    init {
        with(geometry) {
            +leftHandNode
            +rightHandNode
        }
    }

    /**
     * Once the hands are initialized, call this method to add them to the scene.
     */
    protected fun loadHands() {
        with(leftHandNode) { leftHands.forEach { +it } }
        leftHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == 0).ch }

        with(rightHandNode) { rightHands.forEach { +it } }
        rightHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == 0).ch }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        if (isPlaying) {
            if (parent.manager == null) return

            val hands = currentNotePeriod?.let { parent.manager.fingering(it.note) } as Hands?
            hands?.let {
                this.hands = it
                updateHandVisibility(it)
            }
        }
    }

    private fun updateHandVisibility(givenHands: Hands) =
        with(givenHands) {
            leftHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == left).ch }
            rightHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == right).ch }
        }

    override fun toString(): String = super.toString() + debugProperty("hands", hands.toString())
}
