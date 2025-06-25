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
package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.MAX_STICK_IDLE_ANGLE
import org.wysko.midis2jam2.instrument.algorithmic.StickStatus
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.BarState.DOWN
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.BarState.UP
import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.White
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.DIM_GLOW
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private const val MALLET_CASE_SCALE: Float = 0.6666667f
private val RANGE = 21..108

/**
 * The mallet instruments.
 */
class Mallets(
    context: Midis2jam2,
    eventList: List<MidiEvent>,
    private val type: MalletType
) : DecayedInstrument(context, eventList) {

    private val hitsByNote: List<List<NoteEvent.NoteOn>> = RANGE.map { x -> hits.filter { it.note.toInt() == x } }

    private val fakeShadow: Spatial? =
        if (context.isFakeShadows) {
            with(geometry) {
                +context.assetLoader.fakeShadow("Assets/XylophoneShadow.obj", "Assets/XylophoneShadow.png").apply {
                    setLocalScale(2 / 3f)
                    loc = v3(0, -22, 0)
                }
            }
        } else {
            null
        }

    private var bars: List<MalletBar> =
        let {
            var whiteCount = 0
            RANGE.mapIndexed { index, note ->
                val byte = note.toByte()
                if (Key.Color.fromNoteNumber(byte) == White) {
                    MalletBar(byte, whiteCount++, hitsByNote[index])
                } else {
                    MalletBar(byte, note, hitsByNote[index])
                }
            }
        }.onEach { geometry += it.root }

    init {
        placement.loc = v3(18, 0, -5)
        with(geometry) {
            +context.modelD("XylophoneCase.obj", "Black.bmp").apply {
                setLocalScale(MALLET_CASE_SCALE)
            }
        }
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        // Prevent shadow from clipping under stage
        fakeShadow?.let {
            val idealY = (0.5 + (index * 2)).coerceAtLeast(0.5)
            val offset = idealY - it.worldTranslation.y
            it.localTranslation.y += offset.toFloat()
        }

        bars.forEach { it.tick(time, delta) }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        val index = updateInstrumentIndex(delta) - 2
        geometry.loc = v3(-53, 26.5 + (2 * index), 0)
        placement.rot = v3(0, -18 * index, 0)
    }

    /**
     * The type of mallets.
     */
    enum class MalletType(internal val textureFile: String) {
        /**
         * The vibraphone.
         */
        Vibraphone("VibesBar.bmp"),

        /**
         * The marimba.
         */
        Marimba("MarimbaBar.bmp"),

        /**
         * The glockenspiel.
         */
        Glockenspiel("GlockenspielBar.bmp"),

        /**
         * The xylophone.
         */
        Xylophone("XylophoneBar.bmp"),
    }

    /**
     * Represents a single bar on the mallet instrument.
     */
    inner class MalletBar(midiNote: Byte, startPos: Int, events: List<NoteEvent.NoteOn>) {

        internal val root = Node()
        private val bar = with(root) {
            +node()
        }
        private var upBar: Spatial
        private var downBar: Spatial
        private val shadow: Spatial = context.modelD("MalletHitShadow.obj", "Black.bmp")

        private var isRecoiling = false
        private var recoilNow = false

        private val mallet =
            Striker(
                context,
                events,
                context.modelD("XylophoneMalletWhite.obj", type.textureFile),
                sticky = false,
            ).apply {
                // Changes the pivot point of rotation
                offsetStick { it.move(0f, 0f, -2f) }
                node.apply {
                    move(0f, 0f, 2f)
                    scale(MALLET_CASE_SCALE)
                }
            }

        init {
            val scaleFactor = (RANGE.last - midiNote + 20) / 50f

            if (Key.Color.fromNoteNumber(midiNote) == White) {
                upBar = context.modelD("XylophoneWhiteBar.obj", type.textureFile)
                downBar = context.modelD("XylophoneWhiteBarDown.obj", type.textureFile)
                    .apply { (this as Geometry).material.setColor("GlowColor", DIM_GLOW) }

                bar.setLocalScale(0.55f, 1f, 0.5f * scaleFactor)
                root.loc = v3(1.333f * (startPos - 26), 0f, 0f)
                mallet.node.loc = v3(0f, 1.35f, -midiNote / 11.5f + 19)
                shadow.loc = v3(0f, 0.75f, -midiNote / 11.5f + 11)
            } else {
                upBar = context.modelD("XylophoneBlackBar.obj", type.textureFile)
                downBar = context.modelD("XylophoneBlackBarDown.obj", type.textureFile)
                    .apply { (this as Geometry).material.setColor("GlowColor", DIM_GLOW) }

                bar.setLocalScale(0.6f, 0.7f, 0.5f * scaleFactor)
                root.move(1.333f * (midiNote * 0.583f - 38.2f), 0f, -midiNote / 50f + 2.667f)
                mallet.node.loc = v3(0f, 2.6f, midiNote / 12.5f - 2)
                shadow.loc = v3(0f, 2f, midiNote / 12.5f - 10)
            }
            with(root) {
                +bar.apply {
                    +downBar.apply { cullHint = false.ch }
                    +upBar
                }
                +mallet.node
                +shadow.apply { scale(0f) }
            }
        }

        /**
         * Animates the mallet and the bar.
         *
         * @param time the current time
         * @param delta the amount of time since the last frame update
         */
        fun tick(time: Duration, delta: Duration) {
            mallet.tick(time, delta).let {
                if (it.velocity > 0) recoilBar()
                setShadowScale(it)
            }

            if (!isRecoiling) {
                showBar(UP)
                return
            }

            showBar(DOWN)

            // Recoiling now, move the bar down
            if (recoilNow) {
                downBar.loc = v3(0, -0.5, 0)
                recoilNow = false
                return
            }

            // Inch the bar back up
            if (downBar.loc.y < -0.0001) {
                downBar.move(0f, (5 * delta.toDouble(SECONDS)).toFloat(), 0f)
                downBar.loc.y = downBar.loc.y.coerceAtMost(0f)
            } else {
                // Done recoiling, reset
                showBar(UP)
                downBar.loc = Vector3f.ZERO
            }
        }

        private fun setShadowScale(it: StickStatus) {
            shadow.setLocalScale(
                ((1 - Math.toDegrees(it.rotationAngle.toDouble()) / MAX_STICK_IDLE_ANGLE) / 2).toFloat().coerceAtLeast(0.0f)
            )
        }

        private fun recoilBar() {
            isRecoiling = true
            recoilNow = true
        }

        private fun showBar(barState: BarState) {
            upBar.cullHint = (barState == UP).ch
            downBar.cullHint = (barState == DOWN).ch
        }
    }
}

private enum class BarState {
    UP, DOWN
}
