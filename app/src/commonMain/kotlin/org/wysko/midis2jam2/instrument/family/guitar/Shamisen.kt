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
package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD

private const val SHAMISEN_SKIN_TEXTURE = "ShamisenSkin.png"
private const val FORWARD = -0.23126f

/**
 * The Shamisen.
 *
 * @param context The context to the main class.
 * @param events The list of all events that this instrument should be aware of.
 */
class Shamisen(context: PerformanceManager, events: List<MidiEvent>) :
    FrettedInstrument(
        context,
        events,
        StandardFrettingEngine(3, 15, intArrayOf(50, 57, 62)),
        FrettedInstrumentPositioning(
            upperY = 38.814f,
            lowerY = -6.1f,
            restingStrings = arrayOf(Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ),
            upperX = floatArrayOf(-0.5f, 0f, 0.5f),
            lowerX = floatArrayOf(-0.5f, 0f, 0.5f),
            fretHeights = object : FretHeightCalculator {
                override fun calculateScale(fret: Int): Float {
                    return fret * 0.048f // 0 --> 0; 15 --> 0.72
                }
            }
        ),
        3,
        context.modelD("Shamisen.obj", SHAMISEN_SKIN_TEXTURE) to SHAMISEN_SKIN_TEXTURE
    ),
    MultipleInstancesLinearAdjustment {

    override val upperStrings: Array<Spatial> = Array(3) {
        with(geometry) {
            +context.modelD("ShamisenString.obj", SHAMISEN_SKIN_TEXTURE).apply {
                loc = v3(positioning.upperX[it], positioning.upperY, FORWARD)
            }
        }
    }

    override val lowerStrings: List<List<Spatial>> = List(3) { i: Int ->
        List(5) { j: Int ->
            with(geometry) {
                +context.modelD("ShamisenStringBottom$j.obj", SHAMISEN_SKIN_TEXTURE).apply {
                    loc = v3(positioning.lowerX[i], positioning.lowerY, FORWARD)
                    cullHint = false.ch
                }
            }
        }
    }

    override val multipleInstancesDirection: Vector3f = v3(5, -4, 0)

    init {
        placement.run {
            loc = v3(56, 43, -23)
            rot = v3(-5, -46, -33)
        }
    }
}
