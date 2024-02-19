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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffTexture
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelR

/** The long and short percussion whistles. */
class Whistle(
    context: Midis2jam2,
    shortHits: MutableList<MidiNoteOnEvent>,
    longHits: MutableList<MidiNoteOnEvent>,
) : AuxiliaryPercussion(context, (shortHits + longHits).sortedBy { it.time }.toMutableList()) {
    /** The short whistle. */
    private val shortWhistle: PercussionWhistle =
        PercussionWhistle(WhistleLength.SHORT).apply {
            highestLevel.setLocalTranslation(-6f, 43f, -83f)
            highestLevel.localRotation = Quaternion().fromAngles(0f, rad(15.0), 0f)
        }.also {
            geometry.attachChild(it.highestLevel)
        }

    /** The long whistle. */
    private val longWhistle: PercussionWhistle =
        PercussionWhistle(WhistleLength.LONG).apply {
            highestLevel.setLocalTranslation(-2f, 40f, -83f)
            highestLevel.localRotation = Quaternion().fromAngles(0f, rad(15.0), 0f)
        }.also {
            geometry.attachChild(it.highestLevel)
        }

    private val shortEventCollector = EventCollector(context, shortHits)
    private val longEventCollector = EventCollector(context, longHits)

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)
        shortEventCollector.advanceCollectOne(time)?.let {
            shortWhistle.play(0.2)
        }
        longEventCollector.advanceCollectOne(time)?.let {
            longWhistle.play(0.4)
        }
        shortWhistle.tick(delta)
        longWhistle.tick(delta)
    }

    /** The enum Whistle length. */
    enum class WhistleLength {
        /** Short whistle length. */
        SHORT,

        /** Long whistle length. */
        LONG,
    }

    /** A single Whistle. */
    inner class PercussionWhistle(length: WhistleLength) {
        /** The Highest level. */
        val highestLevel: Node = Node()

        /** The Anim node. */
        private val animNode =
            Node().apply {
                highestLevel.attachChild(this)
            }.also {
                it.attachChild(context.modelR("Whistle.obj", "ShinySilver.bmp"))
            }

        /** The Puffer. */
        private val puffer: SteamPuffer =
            SteamPuffer(
                context,
                type = if (length == WhistleLength.LONG) SteamPuffTexture.WHISTLE else SteamPuffTexture.NORMAL,
                scale = 1.0,
                behavior = PuffBehavior.UPWARDS,
            ).apply {
                root.setLocalTranslation(0f, 4f, 0f)
                root.localRotation = Quaternion().fromAngles(0f, -1.57f, 0f)
            }.also {
                animNode.attachChild(it.root)
            }

        /** True if this whistle is currently playing, false otherwise. */
        var playing: Boolean = false

        /** The current amount of progress this whistle has made playing. */
        private var progress = 0.0

        /** How long this whistle should play for. */
        private var duration = 0.0

        /**
         * Plays a note.
         *
         * @param duration the duration
         */
        fun play(duration: Double) {
            playing = true
            progress = 0.0
            this.duration = duration
        }

        /**
         * Tick.
         *
         * @param delta the amount of time since the last frame update
         */
        fun tick(delta: Float) {
            if (progress >= 1) {
                playing = false
                progress = 0.0
            }
            if (playing) {
                progress += delta / duration
                animNode.setLocalTranslation(0f, 2 - 2 * progress.toFloat(), 0f)
            } else {
                animNode.setLocalTranslation(0f, 0f, 0f)
            }
            puffer.tick(delta, playing)
        }
    }
}
