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
package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument.KeyColor.WHITE
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.ShadowController.Companion.shadow

/** Any one of vibraphone, glockenspiel, marimba, or xylophone. */
class Mallets(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>, private val type: MalletType) :
    DecayedInstrument(context, eventList) {

    /** List of lists, where each list contains the strikes corresponding to that bar's MIDI note. */
    private val barStrikes: Array<MutableList<MidiNoteOnEvent>> = Array(MALLET_BAR_COUNT) { ArrayList() }

    /** Each bar of the instrument. There are [MALLET_BAR_COUNT] bars. */
    private var bars: Array<MalletBar>

    private var shadow: Spatial

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Prevent shadow from clipping under stage */
        val idealY = (0.5 + (checkInstrumentIndex() * 2)).coerceAtLeast(0.5)
        val offset = idealY - shadow.worldTranslation.y
        shadow.localTranslation.y += offset.toFloat()

        /* For each bar */
        for ((index, bar) in bars.withIndex()) {
            /* Update the bar */
            bar.tick(delta)

            /* Animate its stick */
            val stickStatus = Stick.handleStick(
                context, bar.malletNode, time, delta,
                barStrikes[index],
                Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X, false
            )

            /* Recoil if just struck */
            if (stickStatus.justStruck()) {
                bar.recoilBar()
            }

            /* Update shadow animation */
            bar.shadow.setLocalScale(((1 - Math.toDegrees(stickStatus.rotationAngle.toDouble()) / Stick.MAX_ANGLE) / 2).toFloat())
        }
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
    inner class MalletBar(midiNote: Int, startPos: Int) {

        /** The model in the up position. */
        private var upBar: Spatial

        /** The model in the down position. */
        private var downBar: Spatial

        /** Contains the entire note geometry. */
        val noteNode: Node = Node()

        /** Contains the mallet. */
        val malletNode: Node = Node()

        /** The small, circular shadow that appears as the mallet is striking. */
        val shadow: Spatial

        /** True if the bar is recoiling, false otherwise. */
        private var barIsRecoiling = false

        /** True if the bar should begin recoiling. */
        private var recoilNow = false

        /** Begins recoiling the bar. */
        fun recoilBar() {
            barIsRecoiling = true
            recoilNow = true
        }

        /**
         * Animates the mallet and the bar.
         *
         * @param delta the amount of time since the last frame update
         */
        fun tick(delta: Float) {
            /* If the bar is recoiling */
            if (barIsRecoiling) {
                /* Hide the up bar, show the down bar */
                upBar.cullHint = Always
                downBar.cullHint = Dynamic

                if (recoilNow) {
                    /* Move the bar all the way down */
                    downBar.setLocalTranslation(0f, -0.5f, 0f)
                } else {
                    /* Recoil the bar */
                    if (downBar.localTranslation.y < -0.0001) {
                        /* Move the bar slightly up */
                        downBar.move(0f, 5 * delta, 0f)

                        /* Don't go any higher than resting position */
                        downBar.localTranslation.apply {
                            y = y.coerceAtMost(0f)
                        }
                    } else {
                        /* We've reached the top, show up bar, hide bottom bar, move down bar to 0 */
                        upBar.cullHint = Dynamic
                        downBar.cullHint = Always
                        downBar.setLocalTranslation(0f, 0f, 0f)
                    }
                }
                recoilNow = false
            } else {
                /* Bar is not recoiling, show up bar, hide down bar */
                upBar.cullHint = Dynamic
                downBar.cullHint = Always
            }
        }

        init {
            /* Load mallet */
            val mallet = context.loadModel("XylophoneMalletWhite.obj", type.textureFile).apply {
                setLocalTranslation(0f, 0f, -2f)
            }
            malletNode.run {
                attachChild(mallet)
                setLocalScale(MALLET_CASE_SCALE)
                localRotation = Quaternion().fromAngles(rad(50.0), 0f, 0f)
                move(0f, 0f, 2f)
            }

            /* Load mallet shadow */
            shadow = context.loadModel("MalletHitShadow.obj", "Black.bmp")

            val barNode = Node()
            if (KeyedInstrument.midiValueToColor(midiNote) == WHITE) {
                val scaleFactor = (RANGE_HIGH - midiNote + 20) / 50f
                upBar = context.loadModel("XylophoneWhiteBar.obj", type.textureFile).also { barNode.attachChild(it) }
                downBar = context.loadModel("XylophoneWhiteBarDown.obj", type.textureFile)
                    .also { barNode.attachChild(it) }

                barNode.setLocalScale(0.55f, 1f, 0.5f * scaleFactor)
                noteNode.move(1.333f * (startPos - 26), 0f, 0f)
                malletNode.setLocalTranslation(0f, 1.35f, -midiNote / 11.5f + 19)
                shadow.setLocalTranslation(0f, 0.75f, -midiNote / 11.5f + 11)
            } else {
                val scaleFactor = (RANGE_HIGH - midiNote + 20) / 50f
                upBar = context.loadModel("XylophoneBlackBar.obj", type.textureFile).also { barNode.attachChild(it) }
                downBar = context.loadModel("XylophoneBlackBarDown.obj", type.textureFile)
                    .also { barNode.attachChild(it) }

                barNode.setLocalScale(0.6f, 0.7f, 0.5f * scaleFactor)
                noteNode.move(1.333f * (midiNote * 0.583f - 38.2f), 0f, -midiNote / 50f + 2.667f)
                malletNode.setLocalTranslation(0f, 2.6f, midiNote / 12.5f - 2)
                shadow.setLocalTranslation(0f, 2f, midiNote / 12.5f - 10)
            }
            downBar.cullHint = Always
            barNode.attachChild(upBar)
            noteNode.attachChild(barNode)
            noteNode.attachChild(malletNode)
            noteNode.attachChild(shadow)
            shadow.setLocalScale(0f)
        }
    }

    companion object {
        /** The mallet case is scaled by this value to appear correct. */
        const val MALLET_CASE_SCALE: Float = 0.667f

        /** The number of bars on the mallets instrument. */
        private const val MALLET_BAR_COUNT = 88

        /** The lowest note mallets can play. */
        private const val RANGE_LOW = 21

        /** The highest note mallets can play. */
        private const val RANGE_HIGH = 108
    }

    init {
        /* Load case */
        context.loadModel("XylophoneCase.obj", "Black.bmp").apply {
            this.setLocalScale(MALLET_CASE_SCALE)
            instrumentNode.attachChild(this)
        }

        /* Initialize all bars */
        val theseBars = ArrayList<MalletBar>()
        var whiteCount = 0
        for (i in 0 until MALLET_BAR_COUNT) {
            if (KeyedInstrument.midiValueToColor(i + RANGE_LOW) == WHITE) {
                theseBars.add(MalletBar(i + RANGE_LOW, whiteCount++))
            } else {
                theseBars.add(MalletBar(i + RANGE_LOW, i))
            }
        }
        bars = theseBars.toTypedArray()

        /* Attach all bars to the instrument */
        bars.forEach { instrumentNode.attachChild(it.noteNode) }

        /* Add all applicable events */
        eventList.forEach {
            if (it is MidiNoteOnEvent && it.note in RANGE_LOW..RANGE_HIGH) {
                barStrikes[it.note - RANGE_LOW].add(it)
            }
        }

        /* Position */
        highestLevel.setLocalTranslation(18f, 0f, -5f)

        /* Add shadow */
        shadow = shadow(context, "Assets/XylophoneShadow.obj", "Assets/XylophoneShadow.png").apply {
            setLocalScale(2 / 3f)
            instrumentNode.attachChild(this)
            setLocalTranslation(0f, -22f, 0f)
        }
    }
}