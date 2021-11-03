/*
 * Copyright (C) 2021 Jacob Wysko
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
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue.collect
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.midi.Midi.SHORT_WHISTLE
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.PuffBehavior
import org.wysko.midis2jam2.particle.SteamPuffer.SteamPuffType
import org.wysko.midis2jam2.util.MatType
import org.wysko.midis2jam2.util.Utils.rad

/** The long and short percussion whistles. */
class Whistle(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : NonDrumSetPercussion(context, hits) {

    /** The short whistle. */
    private val shortWhistle: PercussionWhistle

    /** The long whistle. */
    private val longWhistle: PercussionWhistle
    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val nextHits = collect(hits, context, time)
        nextHits.forEach {
            if (it.note == SHORT_WHISTLE) {
                shortWhistle.play(0.2)
            } else {
                longWhistle.play(0.4)
            }
        }
        shortWhistle.tick(delta)
        longWhistle.tick(delta)

        // Override if still playing
        if (shortWhistle.playing || longWhistle.playing) {
            instrumentNode.cullHint = Spatial.CullHint.Dynamic
        }
    }

    /** The enum Whistle length. */
    enum class WhistleLength {
        /** Short whistle length. */
        SHORT,

        /** Long whistle length. */
        LONG
    }

    /** A single Whistle. */
    inner class PercussionWhistle(length: WhistleLength) {
        /** The Anim node. */
        private val animNode = Node()

        /** The Highest level. */
        val highestLevel = Node()

        /** The Puffer. */
        private val puffer: SteamPuffer

        /** True if this whistle is currently playing, false otherwise. */
        var playing = false

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

        /** Instantiates a new Whistle. */
        init {
            puffer = SteamPuffer(
                context,
                if (length == WhistleLength.LONG) SteamPuffType.WHISTLE
                else SteamPuffType.NORMAL, 1.0, PuffBehavior.UPWARDS
            )
            val whistle = context.loadModel("Whistle.obj", "ShinySilver.bmp", MatType.REFLECTIVE, 0.9f)
            puffer.steamPuffNode.setLocalTranslation(0f, 4f, 0f)
            puffer.steamPuffNode.localRotation = Quaternion().fromAngles(0f, -1.57f, 0f)
            animNode.attachChild(whistle)
            animNode.attachChild(puffer.steamPuffNode)
            highestLevel.attachChild(animNode)
        }
    }

    init {
        shortWhistle = PercussionWhistle(WhistleLength.SHORT)
        longWhistle = PercussionWhistle(WhistleLength.LONG)
        shortWhistle.highestLevel.setLocalTranslation(-6f, 43f, -83f)
        shortWhistle.highestLevel.localRotation = Quaternion().fromAngles(0f, rad(15.0), 0f)
        longWhistle.highestLevel.setLocalTranslation(-2f, 40f, -83f)
        longWhistle.highestLevel.localRotation = Quaternion().fromAngles(0f, rad(15.0), 0f)
        instrumentNode.attachChild(shortWhistle.highestLevel)
        instrumentNode.attachChild(longWhistle.highestLevel)
    }
}