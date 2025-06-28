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

package org.wysko.midis2jam2.world

import org.lwjgl.opengl.GL11
import org.wysko.gervill.JwRealTimeSequencer
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.Midis2jam2
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

internal actual fun getSequencerDrift(context: Midis2jam2): Float {
    val desktopContext = context as DesktopMidis2jam2
    val realTimeSequencer = desktopContext.sequencer as JwRealTimeSequencer

    val timeDrift = (realTimeSequencer.time.seconds - desktopContext.time).absoluteValue
    return timeDrift.toDouble(SECONDS).toFloat()
}

internal actual val GL_RENDERER: String by lazy {
    runCatching { GL11.glGetString(GL11.GL_RENDERER) }.getOrNull() ?: "GL information unknown"
}