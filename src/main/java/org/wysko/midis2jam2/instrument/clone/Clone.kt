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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.world.Axis
import java.util.stream.Collectors

/**
 * [MonophonicInstruments][MonophonicInstrument] use Clones to visualize polyphony on monophonic instruments. A
 * clone is required for
 * each degree of polyphony.
 *
 * The calculation of clones (that is, determining how many clones are needed and which clones should be responsible for
 * each note) is performed in [MonophonicInstrument].
 *
 * @see MonophonicInstrument
 */
abstract class Clone protected constructor(
    /**
     * The [MonophonicInstrument] this clone is associated with.
     */
    @JvmField
    val parent: MonophonicInstrument,
    /**
     * The amount to rotate this instrument by when playing.
     */
    private val rotationFactor: Float,
    /**
     * The axis on which this clone rotates when playing.
     */
    private val rotationAxis: Axis
) {
    /**
     * Used for the rotation while playing.
     */
    @JvmField
    val animNode = Node()

    /**
     * The model node.
     */
    @JvmField
    val modelNode = Node()

    /**
     * The note periods for which this clone should be responsible for animating.
     *
     * @see NotePeriod
     */
    @JvmField
    val notePeriods: MutableList<NotePeriod>

    /**
     * Used for moving with [.indexForMoving].
     */
    @JvmField
    val offsetNode = Node()

    /**
     * The highest level.
     */
    @JvmField
    val highestLevel = Node()

    /**
     * Used for positioning and rotation.
     */
    @JvmField
    val idleNode = Node()

    /**
     * The current note period that is being handled.
     */
    @JvmField
    var currentNotePeriod: NotePeriod? = null

    /**
     * Keeps track of whether or not this clone is currently visible. The 0-clone (the clone at index 0) is always
     * visible, that is if the instrument itself is visible.
     */
    private var isVisible = false

    /**
     * Determines if this clone is playing at a certain point. Since [.notePeriods] is always losing note periods
     * that have fully elapsed, this method is likely not reliable for checking events in the past.
     *
     * @param midiTick the current midi tick
     * @return true if should be playing, false otherwise
     */
    @Contract(pure = true)
    fun isPlaying(midiTick: Long): Boolean {
        return notePeriods.any { midiTick >= it.startTick() && midiTick < it.endTick() }
    }

    /**
     * Determines if this clone is playing at a certain point.
     *
     * @return true if should be playing, false otherwise
     */
    @get:Contract(pure = true)
    val isPlaying: Boolean
        get() = currentNotePeriod != null

    /**
     * Hides or shows this clone.
     *
     * @param indexThis the index of this clone
     */
    private fun hideOrShowOnPolyphony(indexThis: Int) {
        if (indexThis != 0) {
            if (indexForMoving() == 0) {
                isVisible = false
                highestLevel.cullHint = Always
            }
            if (currentNotePeriod != null) {
                highestLevel.cullHint = Dynamic
                isVisible = true
            } else {
                highestLevel.cullHint = Always
                isVisible = false
            }
        } else {
            highestLevel.cullHint = Dynamic
            isVisible = true
        }
    }

    /**
     * Similar to [org.wysko.midis2jam2.instrument.Instrument.tick].
     *
     *  * Calls [hideOrShowOnPolyphony]
     *  * Rotates clone based on playing
     *
     * @param time  the current time
     * @param delta the amount of time since last frame
     */
    open fun tick(time: Double, delta: Float) {
        while (notePeriods.isNotEmpty() && notePeriods[0].startTime <= time) {
            currentNotePeriod = notePeriods.removeAt(0)
        }
        if (currentNotePeriod != null && currentNotePeriod!!.endTime <= time) {
            currentNotePeriod = null
        }

        /* Rotate clone on note play */
        if (currentNotePeriod == null) {
            animNode.localRotation = Quaternion()
        } else {
            val rotate =
                -((currentNotePeriod!!.endTime - time) / currentNotePeriod!!.duration()).toFloat() * rotationFactor
            animNode.localRotation = Quaternion().fromAngles(
                if (rotationAxis === Axis.X) rotate else 0f,
                if (rotationAxis === Axis.Y) rotate else 0f,
                if (rotationAxis === Axis.Z) rotate else 0f
            )
        }
        hideOrShowOnPolyphony(parent.clones.indexOf(this))
        moveForPolyphony()
    }

    /**
     * Returns the index for moving so that clones do not overlap.
     *
     * @return the index for moving so that clones do not overlap
     */
    protected fun indexForMoving(): Int {
        return 0.coerceAtLeast(parent.clones.stream().filter { obj: Clone -> obj.isVisible }
            .collect(Collectors.toList()).indexOf(this))
    }

    /**
     * Move as to not overlap with other clones.
     */
    protected abstract fun moveForPolyphony()

    init {
        /* Connect node chain hierarchy */
        idleNode.attachChild(modelNode)
        animNode.attachChild(idleNode)
        highestLevel.attachChild(animNode)
        offsetNode.attachChild(highestLevel)
        parent.groupOfPolyphony.attachChild(offsetNode)
        notePeriods = ArrayList()
    }
}