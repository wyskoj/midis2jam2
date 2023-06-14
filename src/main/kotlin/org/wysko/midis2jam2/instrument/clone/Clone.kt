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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.NotePeriodCollector
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.util.cullHint
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

    /** Used for pitch bend rotation. */
    val bendNode: Node = Node()

    /** The current note period that is being handled. */
    var currentNotePeriod: NotePeriod? = null

    /** The last played note period for visibility calculation. */
    private var lastNotePeriod: NotePeriod? = null

    /** The note period collector. */
    protected lateinit var notePeriodCollector: NotePeriodCollector

    /**
     * Keeps track of whether this clone is currently visible. The 0-clone (the clone at index 0) is always
     * visible, that is if the instrument itself is visible.
     */
    var isVisible = false

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

        notePeriodCollector.prev()?.let {
            if (notePeriods.isNotEmpty() && (
                        notePeriodCollector.peek()?.startTick()?.minus(it.endTick())
                            ?: Long.MAX_VALUE
                        ) <= parent.context.file.division * 2
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
            if (indexForMoving() == 0f) {
                isVisible = false
                highestLevel.cullHint = Always
            }
            /* A further check if currently playing, show, hide otherwise. */
            highestLevel.cullHint = calcVisibility().cullHint()
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
        currentNotePeriod = notePeriodCollector.advance(time).firstOrNull()

        /* Rotate clone on note play */
        currentNotePeriod?.let {
            val rotate = -((it.endTime - time) / it.duration()).toFloat() * rotationFactor
            animNode.localRotation = Quaternion().fromAngles(
                Matrix3f.IDENTITY.getRow(rotationAxis.componentIndex).mult(rotate).toArray(null)
            )
        } ?: run {
            animNode.localRotation = Quaternion()
        }
        hideOrShowOnPolyphony(parent.clones.indexOf(this))
        moveForPolyphony(delta)
    }

    /**
     * Returns the index for moving so that clones do not overlap.
     *
     * This returns the index of this clone in the list of currently visible clones, where the index is never less
     * than 0.
     */
    protected open fun indexForMoving(): Float =
        0f.coerceAtLeast(parent.clones.filter { it.isVisible }.indexOf(this).toFloat())

    /** Move as to not overlap with other clones. */
    protected abstract fun moveForPolyphony(delta: Float)

    init {
        /* Connect node chain hierarchy */
        idleNode.attachChild(modelNode)
        animNode.attachChild(idleNode)
        bendNode.attachChild(animNode)
        highestLevel.attachChild(bendNode)
        offsetNode.attachChild(highestLevel)
        parent.groupOfPolyphony.attachChild(offsetNode)
    }

    /** Formats a property about this instrument for debugging purposes. */
    protected fun debugProperty(name: String, value: String): String {
        return "\t\t- $name: $value\n"
    }

    /** Formats a property about this instrument for debugging purposes. */
    protected fun debugProperty(name: String, value: Float): String {
        return "\t\t- $name: ${"%.3f".format(value)}\n"
    }

    override fun toString(): String {
        return "\t- ${javaClass.simpleName}\n"
    }

    /** Initializes [notePeriodCollector]. */
    fun createCollector() {
        notePeriodCollector = NotePeriodCollector(notePeriods, parent.context)
    }
}

/** Returns a string ready for display of a list of [Clone]s. */
fun List<Clone>.debugString(): String {
    return joinToString(separator = "")
}

/**
 * Defines a configuration for applying pitch bend animation to clones.
 */
data class ClonePitchBendConfiguration(
    /** The axis on which to rotate. */
    val rotationalAxis: Axis = Axis.X,
    /** The scale factor to apply to the bend, changing its intensity. */
    val scaleFactor: Float = 0.05f,
    /** Inverts the calculated bend amount, if true. */
    val reversed: Boolean = false
)
