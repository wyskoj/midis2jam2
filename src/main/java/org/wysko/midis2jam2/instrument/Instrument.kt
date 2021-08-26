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
package org.wysko.midis2jam2.instrument

import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.InstrumentTransition
import kotlin.math.max

/**
 * An *Instrument* is any visual representation of a MIDI instrument. midis2jam2 displays separate instruments for
 * each channel, and also creates new instruments when the program of a channel changes (i.e., the MIDI instrument of
 * the channel changes).
 *
 * Classes that implement Instrument are responsible for handling [tick], which updates the current animation and
 * note handling for every call.
 */
abstract class Instrument protected constructor(
    /**
     * Since these classes are effectively static, we need reference to the main class.
     */
    @JvmField
    val context: Midis2jam2,
) {
    /**
     * The Offset node.
     */
    @JvmField
    val offsetNode = Node()

    /**
     * The Highest level.
     */
    @JvmField
    val highestLevel = Node()

    /**
     * Should contain geometry and nodes for geometry.
     */
    @JvmField
    val instrumentNode = Node()

    /**
     * When true, this instrument should be displayed on the screen. Otherwise, it should not. The positions of
     * instruments rely on this variable (if bass guitar 1 hides after a while, bass guitar 2 should step in to fill its
     * spot).
     */
    var isVisible = false

    /**
     * The index of this instrument in the stack of similar instruments. Can be a decimal when instrument transition
     * easing is enabled.
     */
    private var index = 0.0

    /**
     * Updates the animation and other necessary frame-dependant calculations. Always call super!!
     *
     * @param time  the current time since the beginning of the MIDI file, expressed in seconds
     * @param delta the amount of time since the last call this method, expressed in seconds
     */
    abstract fun tick(time: Double, delta: Float)

    /**
     * Determines whether this instrument should be visible at the time, and sets the visibility accordingly.
     *
     * The instrument should be visible if:
     *  * There is at least 1 second between now and any strike, when the strike comes later, or,
     *  * There is at least 4 seconds between now and any strike, when the strike has elapsed
     *
     * @param strikes the note on events to check from
     * @param time    the current time
     * @param node    the node to hide
     */
    protected fun setIdleVisibilityByStrikes(strikes: List<MidiNoteOnEvent>, time: Double, node: Node) {
        var show = false
        for (strike in strikes) {
            val delta = time - context.file.eventInSeconds(strike)
            if (delta < END_BUFFER && delta > -START_BUFFER) {
                isVisible = true
                show = true
                break
            }
        }
        isVisible = show
        node.cullHint = if (show) Dynamic else Always
    }

    /**
     * Returns the index of this instrument in the list of other instruments of this type that are visible.
     *
     * @param delta the amount of time that has passed since the last frame
     * @return the index of this instrument in the list of other instruments of this type that are visible
     */
    @Contract(pure = true)
    protected fun indexForMoving(delta: Float): Float {
        val target: Int = if (isVisible) {
            max(0, context.instruments
                .filter { e: Instrument -> this.javaClass.isInstance(e) && e.isVisible }
                .toList().indexOf(this))
        } else {
            context.instruments.filter { e: Instrument -> this.javaClass.isInstance(e) && e.isVisible }.count() - 1
        }
        val transitionSpeed = context.settings.transitionSpeed
        return if (transitionSpeed != InstrumentTransition.NONE) {
            val animationCoefficient = transitionSpeed.speed
            index += delta * TRANSITION_SPEED * (target - index) / animationCoefficient
            index.toFloat()
        } else {
            target.toFloat()
        }
    }

    /**
     * Calculates and moves this instrument for when multiple instances of this instrument are visible.
     *
     * @param delta the amount of time that has passed since the last frame
     */
    protected abstract fun moveForMultiChannel(delta: Float)

    companion object {
        /**
         * The number of seconds an instrument should be spawn before its first note.
         */
        const val START_BUFFER = 1f

        /**
         * The number of seconds an instrument should be spawn after its last note.
         */
        const val END_BUFFER = 5f

        /**
         * How fast instruments move when transitioning.
         */
        private const val TRANSITION_SPEED = 2500
    }

    init {
        /* Connect node tree */
        highestLevel.attachChild(instrumentNode)
        offsetNode.attachChild(highestLevel)
        context.rootNode.attachChild(offsetNode)
    }
}