/*
 * Copyright (C) 2026 Jacob Wysko
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
import com.jme3.input.controls.ActionListener
import org.wysko.midis2jam2.manager.ActionsManager.Companion.ACTION_FRETTING_DEBUG

/**
 * Toggles the live fretting readout that floats above every fretted instrument (F4).
 *
 * Kept apart from the F3 debug overlay, which darkens the whole scene: the point of this readout is to watch the
 * instruments while they play.
 *
 * @see org.wysko.midis2jam2.instrument.family.guitar.FrettingDebugOverlay
 */
class FrettingDebugManager : BaseManager(), ActionListener {
    /** Whether fretted instruments show their readout. */
    var isReadoutVisible: Boolean = false
        private set

    override fun initialize(app: Application) {
        super.initialize(app)
        app.inputManager.addListener(this, ACTION_FRETTING_DEBUG)
    }

    override fun cleanup(app: Application?) {
        app?.inputManager?.removeListener(this)
    }

    override fun onAction(name: String?, isPressed: Boolean, tpf: Float) {
        if (isPressed && name == ACTION_FRETTING_DEBUG) isReadoutVisible = !isReadoutVisible
    }
}
