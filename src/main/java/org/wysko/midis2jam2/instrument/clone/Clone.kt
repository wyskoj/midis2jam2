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
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.world.Axis

/**
 * An instance of an instrument that is used to display degrees of polyphony on a [MonophonicInstrument].
 *
 * Monophonic instruments cannot play more than one note at a time, so a duplicate of the original instrument is
 * temporarily created to play the note. The number of clones required to play the note is determined by the degree
 * of polyphony of the notes in the original instrument.
 *
 * @see MonophonicInstrument
 */
abstract class Clone protected constructor(
    /** The [MonophonicInstrument] this clone is associated with. */
    val parent: MonophonicInstrument,

    /** The amount to rotate this instrument by when playing. */
    private val rotationFactor: Float,

    /** The axis on which this clone rotates when playing. */
    private val rotationAxis: Axis
) {
    /** Used for the rotation while playing. */
    val animNode: Node = Node()

    /** The model node. */
    val modelNode: Node = Node()

    /** The note periods for which this clone should be responsible for animating. */
    val notePeriods: MutableList<NotePeriod> = ArrayList()

    /** Used for moving with [indexForMoving]. */
    val offsetNode: Node = Node()

    /** The highest level. */
    val highestLevel: Node = Node()

    /** Used for positioning and rotation. */
    val idleNode: Node = Node()

    /** The current note period that is being handled. */
    var currentNotePeriod: NotePeriod? = null

    /** The last played note period for visibility calculation. */
    var lastNotePeriod: NotePeriod? = null

    /**
     * Keeps track of whether this clone is currently visible. The 0-clone (the clone at index 0) is always
     * visible, that is if the instrument itself is visible.
     */
    private var isVisible = false

    /** Determines if this clone is playing. */
    @get:Contract(pure = true)
    val isPlaying: Boolean
        get() = currentNotePeriod != null

    /**
     * Determines if this clone is visible when this method is called. This clone is visible if it is currently
     * playing, or it is in between two notes, where the distance from the end of the last note to the start of the
     * next note is less than or equal to 2 half-notes.
     */
    private fun calcVisibility(): Boolean {
        if (currentNotePeriod != null) return true

        lastNotePeriod?.let {
            if (notePeriods.isNotEmpty()
                && notePeriods.first().startTick() - it.endTick() <= parent.context.file.division * 2
            ) {
                return true
            }
        }
        return false
    }

    /** Hides or shows this clone, given the [index]. */
    private fun hideOrShowOnPolyphony(index: Int) {
        /* If this is the 0-clone, always show. */
        if (index == 0) {
            highestLevel.cullHint = Dynamic
            isVisible = true
        } else {
            /* If we are currently positioned at 0, but normally we wouldn't be, hide. */
            if (indexForMoving() == 0) {
                isVisible = false
                highestLevel.cullHint = Always
            }
            /* A further check if currently playing, show, hide otherwise. */
            highestLevel.cullHint = Utils.cullHint(calcVisibility())
            isVisible = calcVisibility()
        }
    }

    /**
     * Updates the clone on every frame.
     *
     * The base implementation performs the following:
     *
     * * Uses [NoteQueue] to determine the current note period.
     * * Clears the [currentNotePeriod] if it has elapsed.
     * * Rotates the clone using the [animNode].
     * * Updates the visibility of the clone.
     * * Updates the position of the clone.
     */
    open fun tick(time: Double, delta: Float) {
        /* Grab the newest note period */
        NoteQueue.collectOne(notePeriods, time)?.let { currentNotePeriod = it }

        /* Clear the note period if it is elapsed */
        currentNotePeriod?.let {
            if (it.endTime <= time) {
                lastNotePeriod = currentNotePeriod
                currentNotePeriod = null
            }
        }

        /* Rotate clone on note play */
        currentNotePeriod?.let {
            val rotate = -((it.endTime - time) / it.duration()).toFloat() * rotationFactor
            animNode.localRotation = Quaternion().fromAngles(
                if (rotationAxis === Axis.X) rotate else 0f,
                if (rotationAxis === Axis.Y) rotate else 0f,
                if (rotationAxis === Axis.Z) rotate else 0f
            )
        } ?: run {
            animNode.localRotation = Quaternion()
        }
        hideOrShowOnPolyphony(parent.clones.indexOf(this))
        moveForPolyphony()
    }

    /**
     * Returns the index for moving so that clones do not overlap.
     *
     * This returns the index of this clone in the list of currently visible clones, where the index is never less
     * than 0.
     */
    protected fun indexForMoving(): Int = 0.coerceAtLeast(parent.clones.filter { it.isVisible }.indexOf(this))

    /** Move as to not overlap with other clones. */
    protected abstract fun moveForPolyphony()

    init {
        /* Connect node chain hierarchy */
        idleNode.attachChild(modelNode)
        animNode.attachChild(idleNode)
        highestLevel.attachChild(animNode)
        offsetNode.attachChild(highestLevel)
        parent.groupOfPolyphony.attachChild(offsetNode)
    }
}