/*
 * Copyright (C) 2023 Jacob Wysko
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
@file:Suppress("unused")

package org.wysko.midis2jam2.instrument.family.percussion

/**
 * Defines the percussion kits available in the MIDI standard.
 */
enum class PercussionKit(
    /**
     * The program number of this kit.
     */
    val midiNumber: Int
) {
    /**
     * Standard kit.
     */
    STANDARD(0),

    /**
     * Room kit.
     */
    ROOM(8),

    /**
     * Power kit.
     */
    POWER(16),

    /**
     * Electronic kit.
     */
    ELECTRONIC(24),

    /**
     * Analog kit.
     */
    ANALOG(25),

    /**
     * Jazz kit.
     */
    JAZZ(32),

    /**
     * Brush kit.
     */
    BRUSH(40),

    /**
     * Orchestra kit.
     */
    ORCHESTRA(48),

    /**
     * SFX kit.
     */
    SFX(56)
}
