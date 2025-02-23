package org.wysko.midis2jam2

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
