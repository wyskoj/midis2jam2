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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.instrument.MonophonicInstrument
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.world.Axis
import kotlin.time.Duration

/**
 * An instance of an instrument that is used to display degrees of polyphony on a [MonophonicInstrument].
 *
 * Monophonic instruments can't play more than one note at a time, so a duplicate of the original instrument is
 * temporarily created to play the note. The number of clones required to play the note is determined by the degree
 * of polyphony in the original instrument.
 *
 * @property parent The parent instrument.
 * @param rotationFactor The factor by which to rotate the clone.
 * @param rotationAxis The axis on which to rotate the clone.
 * @see MonophonicInstrument
 */
abstract class Clone protected constructor(
    val parent: MonophonicInstrument,
    private val rotationFactor: Float,
    private val rotationAxis: Axis
) {
    /**
     * Keeps track of whether this clone is currently visible.
     * The clone at index 0 is always visible (if the parent instrument is visible).
     */
    var isVisible: Boolean = false

    /** The note periods for which this clone should be responsible for animating. */
    val arcs: MutableList<TimedArc> = ArrayList()

    /** Used for moving with [indexForMoving]. */
    val root: Node = Node()

    /** The model node. */
    val geometry: Node = Node()

    /** Used for the rotation while playing. */
    val animNode: Node = Node()

    /** The highest level. */
    val highestLevel: Node = Node()

    /** Used for pitch bend rotation. */
    val bendNode: Node = Node()

    /** The current note period that is being handled. */
    var currentNotePeriod: TimedArc? = null

    /** The note period collector. */
    protected lateinit var timedArcCollector: TimedArcCollector

    /** Determines if this clone is playing. */
    val isPlaying: Boolean
        get() = currentNotePeriod != null

    init {
        parent.geometry += root
        root += highestLevel
        highestLevel += bendNode
        bendNode += animNode
        animNode += geometry
    }

    /**
     * Updates the clone on every frame.
     *
     * The base implementation performs the following:
     *
     * * Uses [TimedArcCollector] to determine the [currentNotePeriod].
     * * Clears the [currentNotePeriod] if it has elapsed.
     * * Rotates the clone using the [animNode].
     * * Updates the visibility of the clone.
     * * Updates the position of the clone.
     */
    open fun tick(time: Duration, delta: Duration) {
        currentNotePeriod = timedArcCollector.advance(time).currentTimedArcs.firstOrNull()

        currentNotePeriod?.let {
            val rotate = -((it.endTime - time) / it.duration).toFloat() * rotationFactor
            animNode.localRotation = Quaternion().fromAngles(
                Matrix3f.IDENTITY.getRow(rotationAxis.componentIndex).mult(rotate).toArray(null)
            )
        } ?: run {
            animNode.localRotation = Quaternion()
        }
        hideOrShowOnPolyphony(parent.clones.indexOf(this))
        adjustForPolyphony(delta)
    }

    /** Initializes [timedArcCollector]. */
    open fun createCollector() {
        timedArcCollector = TimedArcCollector(parent.context, arcs)
    }

    /**
     * Returns the index for moving so that clones don't overlap.
     *
     * This returns the index of this clone in the list of currently visible clones, where the index is never less
     * than 0.
     */
    protected open fun indexForMoving(): Float =
        0f.coerceAtLeast(parent.clones.filter { it.isVisible }.indexOf(this).toFloat())

    /** Move as to not overlap with other clones. */
    protected abstract fun adjustForPolyphony(delta: Duration)

    private fun calcVisibility(): Boolean {
        if (currentNotePeriod != null) return true

        timedArcCollector.prev()?.let { prev ->
            val timeGap = timedArcCollector.peek()?.start?.minus(prev.end) ?: Int.MAX_VALUE
            if (timeGap <= parent.context.sequence.smf.tpq * 2) {
                return true
            }
        }
        return false
    }

    private fun hideOrShowOnPolyphony(index: Int) {
        if (index == 0) {
            // The 0-clone is always visible
            highestLevel.cullHint = Dynamic
            isVisible = true
        } else {
            // If we're currently positioned at 0, but normally we wouldn't be, hide.
            if (indexForMoving() == 0f) {
                isVisible = false
                highestLevel.cullHint = Always
            }
            // A further check: if currently playing, show, hide otherwise.
            highestLevel.cullHint = calcVisibility().ch
            isVisible = calcVisibility()
        }
    }

    /**
     * Formats a property about this instrument for debugging purposes.
     *
     * @param name The name of the property.
     * @param value The value of the property.
     */
    protected fun debugProperty(name: String, value: Any): String =
        if (value is Float) "\t\t- $name: ${"%.3f".format(value)}\n" else "\t\t- $name: $value\n"

    override fun toString(): String {
        return "\t- ${javaClass.simpleName}\n"
    }
}

/**
 * On a list of [Clone]s, returns a string that contains the debug string of each clone.
 *
 * @return A string that contains the debug string of each clone.
 * @see Clone.debugProperty
 */
fun List<Clone>.debugString(): String = joinToString(separator = "")

/**
 * Defines a configuration for applying pitch bend animation to clones.
 *
 * @property rotationalAxis The axis on which to rotate.
 * @property scaleFactor The scale factor to apply to the bend, changing its intensity.
 * @property reversed Inverts the calculated bend amount, if true.
 */
data class ClonePitchBendConfiguration(
    val rotationalAxis: Axis = Axis.X,
    val scaleFactor: Float = 0.05f,
    val reversed: Boolean = false
)
