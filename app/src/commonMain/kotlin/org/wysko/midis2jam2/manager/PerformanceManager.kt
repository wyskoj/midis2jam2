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

package org.wysko.midis2jam2.manager

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.bounding.BoundingBox
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.debug.WireBox
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.algorithmic.InstrumentAssignment
import org.wysko.midis2jam2.starter.configuration.Configuration

abstract class PerformanceManager(
    val sequence: TimeBasedSequence,
    val fileName: String,
    val configs: Collection<Configuration>,
) : AbstractAppState() {
    val root: Node = Node()
    lateinit var app: SimpleApplication
    lateinit var instruments: List<Instrument>
    abstract val onLoadingProgress: (Float) -> Unit

    override fun initialize(stateManager: AppStateManager, app: Application) {
        this.app = app as SimpleApplication
        this.instruments = InstrumentAssignment.assign(this, sequence, onLoadingProgress)
        super.initialize(stateManager, app)
    }

    override fun update(tpf: Float) {
        super.update(tpf)
    }
}