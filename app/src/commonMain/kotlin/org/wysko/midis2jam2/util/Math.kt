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

package org.wysko.midis2jam2.util

import kotlin.math.pow

private const val SMALL_NUMBER = 1.0e-8f

fun dLerp(a: Number, b: Number, t: Number): Double {
    val fA = a.toDouble()
    val fB = b.toDouble()
    val fT = t.toDouble()
    return fA + fT * (fB - fA)
}

fun fLerp(a: Number, b: Number, t: Number): Float {
    val fA = a.toFloat()
    val fB = b.toFloat()
    val fT = t.toFloat()
    return fA + fT * (fB - fA)
}

fun interpTo(current: Number, target: Number, deltaTime: Number, interpSpeed: Number): Float {
    val fCurrent = current.toFloat()
    val fTarget = target.toFloat()
    val fDeltaTime = deltaTime.toFloat()
    val fInterpSpeed = interpSpeed.toFloat()

    val distance = fTarget - fCurrent

    if (distance.pow(2) < SMALL_NUMBER || fInterpSpeed <= 0f) {
        return fTarget
    }

    val move = distance * (fDeltaTime * fInterpSpeed).coerceIn(0f..1f)

    return fCurrent + move
}

fun mapRangeClamped(value: Number, inMin: Number, inMax: Number, outMin: Number, outMax: Number): Float {
    val fValue = value.toFloat()
    val fInMin = inMin.toFloat()
    val fInMax = inMax.toFloat()
    val fOutMin = outMin.toFloat()
    val fOutMax = outMax.toFloat()

    if (fInMin == fInMax) {
        return fOutMin
    }

    val result = (fValue - fInMin) / (fInMax - fInMin) * (fOutMax - fOutMin) + fOutMin

    val lowerBound = minOf(fOutMin, fOutMax)
    val upperBound = maxOf(fOutMin, fOutMax)

    return result.coerceIn(lowerBound, upperBound)
}

fun easeOut(x: Float): Float = if (x == 1f) 1.0f else (1 - 2.0f.pow(-10 * x))

const val PITCHES = 12

@Suppress("MagicNumber")
fun noteNumberToPitch(noteNumber: Int): String = when (noteNumber % PITCHES) {
    0 -> "C"
    1 -> "C#"
    2 -> "D"
    3 -> "D#"
    4 -> "E"
    5 -> "F"
    6 -> "F#"
    7 -> "G"
    8 -> "G#"
    9 -> "A"
    10 -> "A#"
    11 -> "B"
    else -> error("Invalid note number: $noteNumber")
}
