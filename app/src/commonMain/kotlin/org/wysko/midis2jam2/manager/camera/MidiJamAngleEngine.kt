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

package org.wysko.midis2jam2.manager.camera

import org.wysko.midis2jam2.util.logger

class MidiJamAngleEngine(
    val prohibitedAngles: Set<Angle> = emptySet(),
) {
    var currentAngle: Angle = Angle.Angle1A
        private set

    private var currentCategory: Int = 1

    fun triggerCategory(category: Int): Angle {
        val anglesInCategory = Angle.getAnglesInCategory(category)
        if (anglesInCategory.isEmpty()) {
            logger().warn("Bad camera category")
            return currentAngle
        }

        val availableAngles = anglesInCategory.minus(prohibitedAngles)
        val indexOfCurrentAngle = availableAngles.indexOf(currentAngle)
        currentCategory = category
        if (indexOfCurrentAngle == -1) {
            currentAngle = availableAngles.first()
        } else {
            val nextIndex = (indexOfCurrentAngle + 1) % availableAngles.size
            currentAngle = availableAngles[nextIndex]
        }
        return currentAngle
    }

    enum class Angle(
        val category: Int,
    ) {
        Angle1A(1),
        Angle1B(1),
        Angle1C(1),
        Angle2A(2),
        Angle2B(2),
        Angle3A(3),
        Angle3B(3),
        Angle4A(4),
        Angle4B(4),
        Angle5(5),
        Angle6A(6),
        Angle6B(6);

        companion object {
            fun getAnglesInCategory(category: Int): List<Angle> = entries.filter { it.category == category }
        }
    }
}