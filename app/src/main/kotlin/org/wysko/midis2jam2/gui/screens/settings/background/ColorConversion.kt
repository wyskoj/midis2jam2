package org.wysko.midis2jam2.gui.screens.settings.background

/**
 * Calculates the Hue, Saturation, and Value (HSV) components of an integer color value.
 *
 * @receiver The integer color value.
 * @return A [Triple] containing the calculated Hue (in degrees), Saturation (in the range [0, 1]), and Value (in the range [0, 1]) components.
 */
val Int.hsv: Triple<Float, Float, Float>
    get() {
        val r = this shr 16 and 0xFF
        val g = this shr 8 and 0xFF
        val b = this and 0xFF

        val (max, _, delta) = calculateMaxMinDelta(r, g, b)

        val h = calculateHue(r, g, b, max, delta).wrapToRange(0..360)
        val s = calculateSaturation(max, delta)
        val v = max.toFloat() / 255

        return Triple(h, s, v)
    }

/**
 * Converts a hexadecimal color string to an ARGB integer value.
 *
 * @param hexColor The hexadecimal color string to convert. Must be in the format "#RRGGBB".
 * @return The ARGB integer value corresponding to the given hexadecimal color string, or null if the input is invalid.
 */
fun hexStringToIntArgb(hexColor: String?): Int? {
    return if (hexColor != null && Regex("^#[A-Fa-f0-9]{6}$").matches(hexColor)) {
        // Parse the RGB components and add the alpha component manually as 0xFF
        Integer.parseUnsignedInt(hexColor.drop(1), 16) or (0xFF shl 24)
    } else {
        null
    }
}

private fun calculateMaxMinDelta(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    return Triple(max, min, delta)
}

private fun calculateHue(r: Int, g: Int, b: Int, max: Int, delta: Int): Float = when {
    delta == 0 -> 0f
    max == r -> 60f * ((g - b).toFloat() / delta.toFloat() % 6)
    max == g -> 60f * ((b - r).toFloat() / delta.toFloat() + 2)
    else -> 60f * ((r - g).toFloat() / delta.toFloat() + 4)
}

private fun calculateSaturation(max: Int, delta: Int): Float = if (max == 0) 0f else delta.toFloat() / max

private fun Float.wrapToRange(intRange: IntRange): Float {
    val rangeSize = intRange.run { last - first }
    return ((this - intRange.first) % rangeSize + rangeSize) % rangeSize + intRange.first
}