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

package org.wysko.midis2jam2.instrument.family.brass

import kotlin.reflect.KClass

/**
 * A type of trumpet.
 *
 * @property clazz The class that this type of trumpet uses to represent polyphony.
 */
enum class TrumpetType(val clazz: KClass<out Trumpet.TrumpetClone>) {

    /**
     * The normal, open trumpet.
     */
    Normal(Trumpet.TrumpetClone::class),

    /**
     * The muted trumpet.
     */
    Muted(Trumpet.MutedTrumpetClone::class)
}
