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
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.midis2jam2.manager.PlaybackManager.Companion.time
import org.wysko.midis2jam2.world.lyric.LyricController
import kotlin.time.Duration.Companion.seconds

/**
 * Displays the lyrics embedded in the MIDI file, when it has any.
 *
 * Only attached when the file actually contains lyric events and the setting is on, so a file
 * without lyrics costs nothing.
 */
class LyricManager(private val lyrics: List<MetaEvent.Lyric>) : BaseManager() {

    /** The controller driving the on-screen text, once this manager has initialised. */
    var controller: LyricController? = null
        private set

    override fun initialize(app: Application) {
        super.initialize(app)
        controller = LyricController(context, lyrics)
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        controller?.tick(app.time, tpf.toDouble().seconds)
    }

    companion object {
        /** Every lyric event in [this], in the order they are sung. */
        fun org.wysko.kmidi.midi.TimeBasedSequence.lyricEvents(): List<MetaEvent.Lyric> =
            smf.tracks
                .flatMap { it.events }
                .filterIsInstance<MetaEvent.Lyric>()
                .sortedBy { it.tick }
    }
}
