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
import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands
import org.wysko.midis2jam2.instrument.algorithmic.HandShapeBlend
import org.wysko.midis2jam2.instrument.algorithmic.HandShapeBlend.Blend
import org.wysko.midis2jam2.instrument.family.pipe.InstrumentWithHands
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.world.Axis
import kotlin.time.Duration

/**
 * Some instruments visualize notes by showing hands in different playing positions. To do this, a separate 3D model for
 * each hand in each "shape" is created. Where a hand's shapes share one mesh layout, they become morph targets of a
 * single [MorphedHand], and the fingers move smoothly into each note's shape, arriving as it starts (see
 * [HandShapeBlend]). Otherwise, the shapes are swapped out at each note.
 */
abstract class CloneWithHands protected constructor(parent: InstrumentWithHands, rotationFactor: Float) :
    Clone(parent, rotationFactor, Axis.X) {

    private val leftHandNode = Node()
    private val rightHandNode = Node()
    private var hands: Hands = Hands(0, 0)
    private var leftMorph: MorphedHand? = null
    private var rightMorph: MorphedHand? = null

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
        leftMorph = loadHand(leftHandNode, leftHands)
        rightMorph = loadHand(rightHandNode, rightHands)
    }

    private fun loadHand(node: Node, shapes: List<Spatial>): MorphedHand? {
        node.detachAllChildren()
        MorphedHand.from(shapes)?.let {
            node.attachChild(it.geometry)
            return it
        }
        with(node) { shapes.forEach { +it } }
        shapes.forEachIndexed { index, spatial -> spatial.cullHint = (index == 0).ch }
        return null
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        if (parent.manager == null) return

        if (leftMorph != null || rightMorph != null) morphHands(time)

        if (isPlaying) {
            val hands = currentNotePeriod?.let { parent.manager.fingering(it.note) } as Hands?
            hands?.let {
                this.hands = it
                updateHandVisibility(it)
            }
        }
    }

    /** Moves the morphing hands from the last note's shape toward the next note's. */
    private fun morphHands(time: Duration) {
        // The first note that hasn't started yet; the arcs are in order of start.
        val nextIndex = -(arcs.binarySearch { if (it.startTime <= time) -1 else 1 } + 1)
        val previous = arcs.getOrNull(nextIndex - 1)
        val next = arcs.getOrNull(nextIndex)

        val from = previous?.let { fingering(it) } ?: next?.let { fingering(it) } ?: return
        val to = next?.let { fingering(it) } ?: from
        val t = next?.let { HandShapeBlend.progress(time, it.startTime, previous?.startTime, previous?.endTime) } ?: 0f

        leftMorph?.show(Blend(from.left, to.left, t))
        rightMorph?.show(Blend(from.right, to.right, t))
    }

    private fun fingering(arc: TimedArc): Hands? = parent.manager?.fingering(arc.note) as Hands?

    private fun updateHandVisibility(givenHands: Hands) =
        with(givenHands) {
            if (leftMorph == null) {
                leftHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == left).ch }
            }
            if (rightMorph == null) {
                rightHands.forEachIndexed { index, spatial -> spatial.cullHint = (index == right).ch }
            }
        }

    override fun toString(): String = super.toString() + debugProperty("hands", hands.toString()) +
        (leftMorph ?: rightMorph)?.let {
            debugProperty("morph", "L ${leftMorph?.blend ?: "-"}, R ${rightMorph?.blend ?: "-"}")
        }.orEmpty()
}
