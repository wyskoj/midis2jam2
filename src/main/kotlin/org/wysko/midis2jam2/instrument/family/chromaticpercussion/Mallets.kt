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
package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.Quaternion
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.MAX_STICK_IDLE_ANGLE
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.piano.KeyColor
import org.wysko.midis2jam2.instrument.family.piano.noteToKeyboardKeyColor
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.DIM_GLOW

/** The mallet case is scaled by this value to appear correct. */
const val MALLET_CASE_SCALE: Float = 0.667f

/** The number of bars on the mallets instrument. */
private const val MALLET_BAR_COUNT = 88

/** The lowest note mallets can play. */
private const val RANGE_LOW = 21

/** The highest note mallets can play. */
private const val RANGE_HIGH = 108

/** Any one of vibraphone, glockenspiel, marimba, or xylophone. */
class Mallets(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, private val type: MalletType) :
    DecayedInstrument(context, eventList) {

    /** List of lists, where each list contains the strikes corresponding to that bar's MIDI note. */
    private val barStrikes: Array<List<MidiNoteOnEvent>> = run {
        val hits = eventList.filterIsInstance<MidiNoteOnEvent>()
        Array(MALLET_BAR_COUNT) { idx ->
            hits.filter { it.note == idx + RANGE_LOW }
        }
    }

    /** The fake shadow that sits below the case. */
    private var fakeShadow: Spatial? = if (context.fakeShadows) {
        context.assetLoader.fakeShadow("Assets/XylophoneShadow.obj", "Assets/XylophoneShadow.png").apply {
            instrumentNode.attachChild(this)
            setLocalScale(2 / 3f)
            setLocalTranslation(0f, -22f, 0f)
        }
    } else null

    /** Each bar of the instrument. There are [MALLET_BAR_COUNT] bars. */
    private var bars: Array<MalletBar> = let {
        val bars = ArrayList<MalletBar>()
        var whiteCount = 0
        for (i in 0 until MALLET_BAR_COUNT) {
            if (noteToKeyboardKeyColor(i + RANGE_LOW) == KeyColor.WHITE) {
                bars.add(MalletBar(i + RANGE_LOW, whiteCount++, barStrikes[i]))
            } else {
                bars.add(MalletBar(i + RANGE_LOW, i, barStrikes[i]))
            }
        }
        bars
    }.onEach { bar -> instrumentNode.attachChild(bar.noteNode) }.toTypedArray()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Prevent shadow from clipping under stage */
        fakeShadow?.let {
            val idealY = (0.5 + (checkInstrumentIndex() * 2)).coerceAtLeast(0.5)
            val offset = idealY - it.worldTranslation.y
            it.localTranslation.y += offset.toFloat()
        }

        /* For each bar */
        bars.forEach { it.tick(time, delta) }
    }

    override fun moveForMultiChannel(delta: Float) {
        val i1 = updateInstrumentIndex(delta) - 2
        instrumentNode.setLocalTranslation(-50f, 26.5f + (2 * i1), 0f)
        highestLevel.localRotation = Quaternion().fromAngles(0f, rad(-18.0) * i1, 0f)
    }

    /** The type of mallets. */
    enum class MalletType(internal val textureFile: String) {

        /** The vibraphone. */
        VIBES("VibesBar.bmp"),

        /** The marimba. */
        MARIMBA("MarimbaBar.bmp"),

        /** The glockenspiel. */
        GLOCKENSPIEL("GlockenspielBar.bmp"),

        /** The xylophone. */
        XYLOPHONE("XylophoneBar.bmp");
    }

    /** A single bar out of the 88 for the mallets. */
    inner class MalletBar(midiNote: Int, startPos: Int, events: List<MidiNoteOnEvent>) {

        /** The model in the up position. */
        private var upBar: Spatial

        /** The model in the down position. */
        private var downBar: Spatial

        /** Contains the entire note geometry. */
        val noteNode: Node = Node()

        private val mallet = Striker(
            context = context,
            strikeEvents = events,
            sticky = false,
            stickModel = context.loadModel("XylophoneMalletWhite.obj", type.textureFile)
        ).apply {
            offsetStick { it.move(0f, 0f, -2f) }
            node.apply {
                move(0f, 0f, 2f)
                scale(MALLET_CASE_SCALE)
            }
        }

        /** The small, circular shadow that appears as the mallet is striking. */
        private val shadow: Spatial = context.loadModel("MalletHitShadow.obj", "Black.bmp")

        /** True if the bar is recoiling, false otherwise. */
        private var barIsRecoiling = false

        /** True if the bar should begin recoiling. */
        private var recoilNow = false

        /** Begins recoiling the bar. */
        private fun recoilBar() {
            barIsRecoiling = true
            recoilNow = true
        }

        /**
         * Animates the mallet and the bar.
         *
         * @param delta the amount of time since the last frame update
         */
        fun tick(time: Double, delta: Float) {
            mallet.tick(time, delta).let {
                if (it.velocity > 0) recoilBar()

                shadow.setLocalScale(((1 - Math.toDegrees(it.rotationAngle.toDouble()) / MAX_STICK_IDLE_ANGLE) / 2).toFloat())
            }

            if (barIsRecoiling) { // Hide the up bar, show the down bar
                upBar.cullHint = Always
                downBar.cullHint = Dynamic

                if (recoilNow) { // Move the bar all the way down
                    downBar.setLocalTranslation(0f, -0.5f, 0f)
                } else { // Recoil the bar
                    if (downBar.localTranslation.y < -0.0001) { // Move the bar slightly up
                        downBar.move(0f, 5 * delta, 0f)

                        /* Don't go any higher than resting position */
                        downBar.localTranslation.apply {
                            y = y.coerceAtMost(0f)
                        }
                    } else { // We've reached the top, show up bar, hide bottom bar, move down bar to 0
                        upBar.cullHint = Dynamic
                        downBar.cullHint = Always
                        downBar.setLocalTranslation(0f, 0f, 0f)
                    }
                }
                recoilNow = false
            } else { // Bar is not recoiling, show up bar, hide down bar
                upBar.cullHint = Dynamic
                downBar.cullHint = Always
            }
        }

        init {
            /* Load mallet shadow */

            val barNode = Node()
            if (noteToKeyboardKeyColor(midiNote) == KeyColor.WHITE) {
                val scaleFactor = (RANGE_HIGH - midiNote + 20) / 50f
                upBar = context.loadModel("XylophoneWhiteBar.obj", type.textureFile).also { barNode.attachChild(it) }
                downBar =
                    context.loadModel("XylophoneWhiteBarDown.obj", type.textureFile).apply {
                        (this as Geometry).material.setColor("GlowColor", DIM_GLOW)
                    }.also { barNode.attachChild(it) }

                barNode.setLocalScale(0.55f, 1f, 0.5f * scaleFactor)
                noteNode.move(1.333f * (startPos - 26), 0f, 0f)
                mallet.node.setLocalTranslation(0f, 1.35f, -midiNote / 11.5f + 19)
                shadow.setLocalTranslation(0f, 0.75f, -midiNote / 11.5f + 11)
            } else {
                val scaleFactor = (RANGE_HIGH - midiNote + 20) / 50f
                upBar = context.loadModel("XylophoneBlackBar.obj", type.textureFile).also { barNode.attachChild(it) }
                downBar = context.loadModel("XylophoneBlackBarDown.obj", type.textureFile).apply {
                    (this as Geometry).material.setColor("GlowColor", DIM_GLOW)
                }.also { barNode.attachChild(it) }

                barNode.setLocalScale(0.6f, 0.7f, 0.5f * scaleFactor)
                noteNode.move(1.333f * (midiNote * 0.583f - 38.2f), 0f, -midiNote / 50f + 2.667f)
                mallet.node.setLocalTranslation(0f, 2.6f, midiNote / 12.5f - 2)
                shadow.setLocalTranslation(0f, 2f, midiNote / 12.5f - 10)
            }
            downBar.cullHint = Always
            barNode.attachChild(upBar)
            noteNode.attachChild(barNode)
            noteNode.attachChild(mallet.node)
            noteNode.attachChild(shadow)
            shadow.setLocalScale(0f)
        }
    }

    init { // Load case
        context.loadModel("XylophoneCase.obj", "Black.bmp").apply {
            this.setLocalScale(MALLET_CASE_SCALE)
            instrumentNode.attachChild(this)
        }

        /* Position */
        highestLevel.setLocalTranslation(18f, 0f, -5f)
    }
}
