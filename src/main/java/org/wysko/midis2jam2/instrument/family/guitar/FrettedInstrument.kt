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
package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.NotePeriod
import kotlin.math.abs
import kotlin.math.floor

/**
 * Any instrument that has strings that can be pushed down to change the pitch (e.g., guitar, bass guitar, violin,
 * banjo, etc.)
 */
abstract class FrettedInstrument protected constructor(
	context: Midis2jam2,
	/**
	 * The fretting engine used for this fretted instrument.
	 */
	private val frettingEngine: FrettingEngine,
	events: List<MidiChannelSpecificEvent>,
	/**
	 * The positioning parameters of this fretted instrument.
	 */
	protected val positioning: FrettedInstrumentPositioning,
	/**
	 * The number of strings on this instrument.
	 */
	private val numberOfStrings: Int,
	instrumentBody: Spatial
) : SustainedInstrument(context, events) {
	/**
	 * Each of the idle, upper strings.
	 */
	protected lateinit var upperStrings: Array<Spatial>

	/**
	 * Each of the animated, lower strings, by animation frame.
	 */
	protected lateinit var lowerStrings: Array<Array<Spatial>>

	/**
	 * The yellow dot note fingers.
	 */
	protected lateinit var noteFingers: Array<Spatial>

	/**
	 * Which frame of animation for the animated string to use.
	 */
	protected var frame = 0.0

	override fun tick(time: Double, delta: Float) {
		super.tick(time, delta)
		handleStrings(time, delta)
	}

	/** Returns the height from the top to the bottom of the strings. */
	@Contract(pure = true)
	private fun stringHeight(): Float {
		return positioning.upperY - positioning.lowerY
	}

	/** Performs a lookup and finds the vertical ratio of the [fret] position. */
	@Contract(pure = true)
	protected fun fretToDistance(fret: Int): Float {
		return positioning.fretHeights.calculateScale(fret)
	}

	/**
	 * Animates a string on a given fret.
	 *
	 * @param string the string to animate
	 * @param fret   the fret on this string to animate
	 */
	private fun animateString(string: Int, fret: Int) {
		if (fret == -1) {
			// Just hide everything
			upperStrings[string].localScale = positioning.restingStrings[string]
			for (anim in lowerStrings[string]) {
				anim.cullHint = Spatial.CullHint.Always
			}
			noteFingers[string].cullHint = Spatial.CullHint.Always
			return
		}
		val fretDistance = fretToDistance(fret)
		val localScale = Vector3f(positioning.restingStrings[string])
		localScale.setY(fretDistance)
		upperStrings[string].localScale = localScale
		for (i in 0..4) {
			frame %= 5
			if (i.toDouble() == floor(frame)) {
				lowerStrings[string][i].cullHint = Spatial.CullHint.Dynamic
			} else {
				lowerStrings[string][i].cullHint = Spatial.CullHint.Always
			}
			lowerStrings[string][i].localScale = Vector3f(positioning.restingStrings[string]).setY(1 - fretDistance)
		}

		// Show the fret finger on the right spot (if not an open string)
		if (fret != 0) {
			noteFingers[string].cullHint = Spatial.CullHint.Dynamic
			val fingerPosition: Vector3f = if (positioning is FrettedInstrumentPositioningWithZ) {
				val positioningWithZ = positioning
				val z = ((positioningWithZ.topZ[string] - positioningWithZ.bottomZ[string])
						* fretDistance + positioningWithZ.topZ[string]) * -1.3f - 2
				Vector3f(
					(positioningWithZ.lowerX[string] - positioningWithZ.upperX[string])
							* fretDistance + positioningWithZ.upperX[string],
					positioningWithZ.fingerVerticalOffset.y - stringHeight() * fretDistance,
					z
				)
			} else {
				Vector3f(
					(positioning.lowerX[string] - positioning.upperX[string]) * fretDistance + positioning.upperX[string],
					positioning.fingerVerticalOffset.y - stringHeight() * fretDistance,
					0f
				)
			}
			noteFingers[string].localTranslation = fingerPosition
		} else {
			noteFingers[string].cullHint = Spatial.CullHint.Always
		}
	}

	override fun calculateCurrentNotePeriods(time: Double) {
		while (notePeriods.isNotEmpty() && notePeriods[0].startTime <= time) {
			currentNotePeriods.add(notePeriods.removeAt(0))
		}
		val r = currentNotePeriods.iterator()
		while (r.hasNext()) {
			val next = r.next()
			if (abs(next.endTime - time) < 0.02 || time > next.endTime) {
				r.remove()
				val next1 = next as NotePeriodWithFretboardPosition
				val string = next1.position!!.string
				if (string != -1) {
					frettingEngine.releaseString(string)
				}
			}
		}
	}

	/**
	 * Performs the calculations and necessary algorithmic processes to correctly show fretted animation.
	 *
	 * @param time  the current time
	 * @param delta the time since the last frame
	 * @return true if a new note was played, false otherwise
	 */
	protected open fun handleStrings(time: Double, delta: Float): Boolean {
		var noteStarted = false
		for (i in 0 until numberOfStrings) {
			val first = currentNotePeriods.stream()
				.filter { notePeriod: NotePeriod -> (notePeriod as NotePeriodWithFretboardPosition).position!!.string == i }
				.findFirst()
			if (first.isPresent) {
				val position = (first.get() as NotePeriodWithFretboardPosition).position
				if (position!!.string != -1 && position.fret != -1) {
					frettingEngine.applyFretboardPosition(position)
				}
			} else {
				frettingEngine.releaseString(i)
			}
		}
		for (notePeriod in currentNotePeriods) {
			if (notePeriod.animationStarted) {
				continue
			}
			val notePeriod1 = notePeriod as NotePeriodWithFretboardPosition
			noteStarted = true
			val guitarPosition = frettingEngine.bestFretboardPosition(notePeriod1.midiNote)
			if (guitarPosition != null) {
				frettingEngine.applyFretboardPosition(guitarPosition)
				notePeriod1.position = guitarPosition
			}
			notePeriod1.animationStarted = true
		}

		/* Animate strings */
		for (i in 0 until numberOfStrings) {
			animateString(i, frettingEngine.frets[i])
		}
		val inc = (delta / (1 / 60f)).toDouble()
		frame += inc
		return noteStarted
	}

	init {
		instrumentNode.attachChild(instrumentBody)
		highestLevel.attachChild(instrumentNode)
		notePeriods = notePeriods.map { NotePeriodWithFretboardPosition.fromNotePeriod(it) }.toMutableList()
	}
}