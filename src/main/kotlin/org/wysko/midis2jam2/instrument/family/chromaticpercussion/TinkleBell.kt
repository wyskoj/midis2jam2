package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.world.GlowController

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

private val OFFSET_DIRECTION_VECTOR = Vector3f(0f, 20f, 0f)

/**
 * The Tinkle Bell.
 */
class TinkleBell(
    context: Midis2jam2,
    events: List<MidiChannelSpecificEvent>
) : DecayedInstrument(context, events) {

    private val twelfths: Array<ATinkleBell> = Array(12) {
        ATinkleBell(it).apply {
            instrumentNode.attachChild(this.bellNode)
        }
    }

    private val bellStrikes = Array(12) { idx ->
        hits.filter { (it.note + 3) % 12 == idx }.toMutableList()
    }

    init {
        instrumentNode.setLocalTranslation(20f, 30f, 10f)
        instrumentNode.localRotation = Quaternion().fromAngles(0f, 2.7f, 0f)
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localTranslation = OFFSET_DIRECTION_VECTOR.mult(updateInstrumentIndex(delta))
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        bellStrikes.forEachIndexed { index, _ ->
            val results = Stick.handleStick(
                context,
                stickNode = twelfths[index].bellNode,
                time,
                delta,
                strikes = bellStrikes[index]
            )
            if (results.justStruck()) twelfths[index].strike()

            twelfths[index].bellNode.cullHint = Spatial.CullHint.Dynamic // Override stick cullHint
        }

        twelfths.forEach { it.tick(delta) }
    }

    private inner class ATinkleBell(index: Int) {

        val bellNode = Node().apply {
            move(index * -4f, 0f, 0f)
        }

        private val outerBell =
            context.loadModel("TinkleBell.obj", "Wood.bmp").also {
                bellNode.attachChild(it)
                it.move(0f, -10f, 0f)

                ((it as Node).children[0] as Geometry).material = context.reflectiveMaterial("HornSkin.bmp")
            }

        private val bell = context.loadModel("TinkleBellBell.obj", "HornSkinGrey.bmp", 1f).also {
            it.move(0f, -7.8f, 0f)
            bellNode.attachChild(it)
        }

        private val cymbalAnimator = CymbalAnimator(1.0, 15.0, 2.0)
        private val glowController = GlowController(glowColor = ColorRGBA.Yellow.mult(0.75f))

        fun tick(delta: Float) {
            cymbalAnimator.tick(delta)
            bell.localRotation = Quaternion().fromAngles(cymbalAnimator.rotationAmount(), 0f, 0f)

            ((outerBell as Node).children[0] as Geometry).material.setColor(
                "GlowColor",
                glowController.calculate(
                    cymbalAnimator.animTime.let { if (it == -1.0) Double.MAX_VALUE else it } * 2f
                )
            )
        }

        fun strike() {
            cymbalAnimator.strike()
        }
    }
}
