/*
 * Copyright (C) 2022 Jacob Wysko
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

enum class PercussionKit(val midiNumber: Int) {
    STANDARD(0),
    ROOM(8),
    POWER(16),
    ELECTRONIC(24),
    ANALOG(25),
    JAZZ(32),
    BRUSH(40),
    ORCHESTRA(48),
    SFX(56);
}