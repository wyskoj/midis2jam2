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

package org.wysko.midis2jam2.ui.common.util

import kotlin.math.pow
import kotlin.math.sqrt

object ColorName {
    fun forColor(argb: Int): String {
        val color = argb and 0x00FFFFFF
        val closestColor = colorTable.minByOrNull { (_, value) -> calculateColorDistance(color, value) }!!
        return closestColor.key
    }
}

// Function to calculate the Euclidean distance between two colors
private fun calculateColorDistance(color1: Int, color2: Int): Double {
    val r1 = color1 shr 16 and 0xFF
    val g1 = color1 shr 8 and 0xFF
    val b1 = color1 and 0xFF

    val r2 = color2 shr 16 and 0xFF
    val g2 = color2 shr 8 and 0xFF
    val b2 = color2 and 0xFF

    return sqrt((r1 - r2).toDouble().pow(2) + (g1 - g2).toDouble().pow(2) + (b1 - b2).toDouble().pow(2))
}


private val colorTable = mapOf(
    "Alice blue" to -984833,
    "Antique white" to -332841,
    "Aqua" to -16711681,
    "Aqua marine" to -8388652,
    "Azure" to -983041,
    "Beige" to -657956,
    "Bisque" to -6972,
    "Black" to -16777216,
    "Blanched almond" to -5171,
    "Blue" to -16776961,
    "Blue violet" to -7722014,
    "Brown" to -5952982,
    "Burly wood" to -2180985,
    "Cadet blue" to -10510688,
    "Chartreuse" to -8388864,
    "Chocolate" to -2987746,
    "Coral" to -32944,
    "Corn flower blue" to -10185235,
    "Cornsilk" to -1828,
    "Crimson" to -2354116,
    "Cyan" to -16711681,
    "Dark blue" to -16777077,
    "Dark cyan" to -16741493,
    "Dark golden rod" to -4684277,
    "Dark gray" to -5658199,
    "Dark green" to -16751616,
    "Dark khaki" to -4343957,
    "Dark magenta" to -7667573,
    "Dark olive green" to -11179217,
    "Dark orange" to -29696,
    "Dark orchid" to -6737204,
    "Dark red" to -7667712,
    "Dark salmon" to -1468806,
    "Dark sea green" to -7357297,
    "Dark slate blue" to -12042869,
    "Dark slate gray" to -13676721,
    "Dark turquoise" to -16724271,
    "Dark violet" to -7077677,
    "Deep pink" to -60269,
    "Deep sky blue" to -16728065,
    "Dim gray" to -9868951,
    "Dodger blue" to -14774017,
    "Fire brick" to -5103070,
    "Floral white" to -1296,
    "Forest green" to -14513374,
    "Fuchsia" to -65281,
    "Gainsboro" to -2302756,
    "Ghost white" to -460545,
    "Gold" to -10496,
    "Golden rod" to -2448096,
    "Gray" to -8355712,
    "Green" to -16744448,
    "Green yellow" to -5374161,
    "Honey dew" to -983056,
    "Hot pink" to -38476,
    "Indian red" to -3318692,
    "Indigo" to -11861886,
    "Ivory" to -16,
    "Khaki" to -989556,
    "Lavender" to -1644806,
    "Lavender blush" to -3851,
    "Lawn green" to -8586240,
    "Lemon chiffon" to -1331,
    "Light blue" to -5383962,
    "Light coral" to -1015680,
    "Light cyan" to -2031617,
    "Light goldenrod yellow" to -329006,
    "Light gray" to -2894893,
    "Light green" to -7278960,
    "Light pink" to -18751,
    "Light salmon" to -24454,
    "Light sea green" to -14634326,
    "Light sky blue" to -7876870,
    "Light slate gray" to -8943463,
    "Light steel blue" to -5192482,
    "Light yellow" to -32,
    "Lime" to -16711936,
    "Lime green" to -13447886,
    "Linen" to -331546,
    "Magenta" to -65281,
    "Maroon" to -8388608,
    "Medium aqua marine" to -10039894,
    "Medium blue" to -16777011,
    "Medium orchid" to -4565549,
    "Medium purple" to -7114536,
    "Medium sea green" to -12799119,
    "Medium slate blue" to -8689426,
    "Medium spring green" to -16713062,
    "Medium turquoise" to -12004916,
    "Medium violet red" to -3730043,
    "Midnight blue" to -15132304,
    "Mint cream" to -655366,
    "Misty rose" to -6943,
    "Moccasin" to -6987,
    "Navajo white" to -8531,
    "Navy" to -16777088,
    "Old lace" to -133658,
    "Olive" to -8355840,
    "Olive drab" to -9728477,
    "Orange" to -23296,
    "Orange red" to -47872,
    "Orchid" to -2461482,
    "Pale golden rod" to -1120086,
    "Pale green" to -6751336,
    "Pale turquoise" to -5247250,
    "Pale violet red" to -2396013,
    "Papaya whip" to -4139,
    "Peach puff" to -9543,
    "Peru" to -3308225,
    "Pink" to -16181,
    "Plum" to -2252579,
    "Powder blue" to -5185306,
    "Purple" to -8388480,
    "Red" to -65536,
    "Rosy brown" to -4419697,
    "Royal blue" to -12490271,
    "Saddle brown" to -7650029,
    "Salmon" to -360334,
    "Sandy brown" to -744352,
    "Sea green" to -13726889,
    "Sea shell" to -2578,
    "Sienna" to -6270419,
    "Silver" to -4144960,
    "Sky blue" to -7876885,
    "Slate blue" to -9807155,
    "Slate gray" to -9404272,
    "Snow" to -1286,
    "Spring green" to -16711809,
    "Steel blue" to -12156236,
    "Tan" to -2968436,
    "Teal" to -16744320,
    "Thistle" to -2572328,
    "Tomato" to -40121,
    "Turquoise" to -12525360,
    "Violet" to -1146130,
    "Wheat" to -663885,
    "White" to -1,
    "White smoke" to -657931,
    "Yellow" to -256,
    "Yellow green" to -6632142
)