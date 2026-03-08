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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import org.wysko.midis2jam2.world.modelR
import kotlin.time.Duration

/**
 * The Metronome.
 *
 * @param context The context to the main class.
 * @param clickHits The hits to be played when the metronome clicks.
 * @param bellHits The hits to be played when the metronome rings.
 */
class Metronome(context: PerformanceManager, clickHits: List<NoteEvent.NoteOn>, bellHits: List<NoteEvent.NoteOn>) :
    AuxiliaryPercussion(context, (clickHits + bellHits).sortedBy { it.tick }) {

    private val bellStriker = Striker(
        context = context, strikeEvents = bellHits, stickModel = Node(), fixed = true, lift = false
    )

    private val clickStriker = Striker(
        context = context, strikeEvents = clickHits, stickModel = Node(), fixed = true, lift = false
    )

    private val bell = context.modelR("MetronomePendjulum2.obj", "HornSkin.bmp").apply {
        loc = v3(0, 0, 0.5)
    }

    private val click = context.modelR("MetronomePendjulum1.obj", "ShinySilver.bmp").apply {
        loc = v3(0, 0, 1)
    }

    private var clickSwingsRight = true
    private var bellSwingsRight = true

    init {
        with(geometry) {
            +bell
            +click
            +context.modelD("MetronomeBox.obj", "Wood.bmp")
        }
        with(placement) {
            loc = v3(-20, 0, -48)
            rot = v3(0, 20, 0)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        with(bellStriker.tick(time, delta)) {
            strike?.let { bellSwingsRight = !bellSwingsRight }
            bell.rot = v3(0, 0, bellSwingsRight.sign * rotationAngle * 20)
        }

        with(clickStriker.tick(time, delta)) {
            strike?.let { clickSwingsRight = !clickSwingsRight }
            click.rot = v3(0, 0, clickSwingsRight.sign * rotationAngle * 20)
        }
    }
}
