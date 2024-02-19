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
package org.wysko.midis2jam2.instrument

import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Visibility
import org.wysko.midis2jam2.starter.configuration.SettingsConfiguration
import org.wysko.midis2jam2.starter.configuration.getType
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.minusAssign
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.times

/**
 * Represents an instrument.
 *
 * All instruments should extend this class.
 * It is responsible for managing the visibility of the instrument,
 * general positioning, and the instrument's node hierarchy.
 *
 * @property context The context to the main class.
 */
abstract class Instrument(val context: Midis2jam2) {

    /**
     * This node is translated when multiple instances of the same instrument are on the stage.
     * It is at the top of the node hierarchy.
     */
    val root: Node = node()

    /**
     * This node is translated for general positioning of the instrument.
     * It is at the second level of the node hierarchy.
     */
    val placement: Node = node()

    /**
     * This node contains the instrument's geometry.
     * It is at the third level of the node hierarchy—the lowest in the [Instrument] class.
     */
    val geometry: Node = node()

    /**
     * The index of the instrument.
     * This is used for positioning the instrument when multiple instances of the same
     * instrument are on the stage.
     * As instruments move on stage, they do not "snap" to their target position.
     * Instead, they gradually move to their target position, which is calculated by
     * [updateInstrumentIndex].
     *
     */
    var index: Double = 0.0

    /**
     * Whether the instrument is visible.
     * The calculation for visibility is handled by [Visibility].
     *
     * If the [context] has the `instrumentAlwaysVisible` setting enabled, this will always be true.
     *
     * The [root] is attached and detached from the [context]'s root node when this is set.
     *
     * @see Visibility
     */
    open var isVisible: Boolean = true
        get() = context.configs.getType(SettingsConfiguration::class).instrumentsAlwaysVisible || field
        set(value) {
            field = value
            with(context.root) {
                if (context.configs.getType(SettingsConfiguration::class).instrumentsAlwaysVisible || field) {
                    this += root
                } else {
                    this -= root
                }
            }
        }

    init {
        placement += geometry
        root += placement
        context.root += root
    }

    /**
     * Called every frame. All logic for the instrument should be handled here.
     *
     * @param time The current time since the beginning of the song, in seconds.
     * @param delta The amount of time that elapsed since the last frame, in seconds.
     */
    abstract fun tick(time: Double, delta: Float)

    /**
     * Calculates the visibility of the instrument.
     *
     * @param time The current time since the beginning of the song, in seconds.
     * @param future Whether to calculate the visibility for the future.
     */
    abstract fun calculateVisibility(time: Double, future: Boolean = false): Boolean

    /**
     * Called when the instrument's [visibility][isVisible] changes from `false` to `true`.
     */
    protected open fun onEntry(): Unit = Unit

    /**
     * Called when the instrument's [visibility][isVisible] changes from `true` to `false`.
     */
    protected open fun onExit(): Unit = Unit

    /**
     * Adjusts the [root] to account for multiple instances of the same instrument.
     *
     * @param delta The amount of time that elapsed since the last frame, in seconds.
     */
    protected open fun adjustForMultipleInstances(delta: Float) {
        when (this) {
            is MultipleInstancesLinearAdjustment -> {
                root.loc = this.multipleInstancesDirection * updateInstrumentIndex(delta)
            }

            is MultipleInstancesRadialAdjustment -> {
                root.rot = rotationAxis
                    .identity
                    .clone()
                    .mult(baseAngle + rotationAngle * updateInstrumentIndex(delta))
            }
        }
    }

    /**
     * Updates the [index] of the instrument.
     *
     * @param delta The amount of time that elapsed since the last frame, in seconds.
     * @return The new index.
     * @see index
     */
    protected fun updateInstrumentIndex(delta: Float): Float {
        val targetIndex = if (isVisible) {
            findSimilarAndVisible().indexOf(this).coerceAtLeast(0)
        } else {
            findSimilarAndVisible().size - 1
        }
        index += delta * 2500 * (targetIndex - index) / 500.0
        index = index.coerceAtMost(findSimilar().size.toDouble())
        return index.toFloat()
    }

    /**
     * Returns a list of instruments that are of the same type as this instrument.
     */
    protected open fun findSimilar(): List<Instrument> = context.instruments.filter { this::class.isInstance(it) }

    /**
     * Returns a list of instruments that are of the same type as this instrument and are visible.
     */
    protected open fun findSimilarAndVisible(): List<Instrument> = findSimilar().filter { it.isVisible }

    /**
     * Formats a property about this instrument for debugging purposes.
     *
     * @param name The name of the property.
     * @param value The value of the property.
     */
    protected fun formatProperty(name: String, value: Any): String = when (value) {
        is Float -> "\t- $name: ${"%.3f".format(value)}\n"
        else -> "\t- $name: $value\n"
    }

    override fun toString(): String = "* ${this@Instrument.javaClass.simpleName} / ${"%.3f".format(index)}\n"
}
