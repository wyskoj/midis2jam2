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

package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/** The Cuica. */
class Cuica(
    context: PerformanceManager,
    muteHits: MutableList<NoteEvent.NoteOn>,
    openHits: MutableList<NoteEvent.NoteOn>,
) : AuxiliaryPercussion(context, (muteHits + openHits).sortedBy { it.tick }.toMutableList()) {
    private val muteCollector = EventCollector(context, muteHits)
    private val openCollector = EventCollector(context, openHits)

    init {
        // Load drum
        context.modelD("DrumSet_Cuica.obj", "DrumShell_Cuica.png").also {
            geometry.attachChild(it)
            (it as Node).getChild(0).setMaterial(context.assetLoader.diffuseMaterial("Wood.bmp"))
        }
    }

    private val strokeHand = context.modelD("Hand_Cuica.obj", "hands.bmp").also {
        geometry.attachChild(it)
    }

    private val restHand = context.modelD("hand_left.obj", "hands.bmp").also {
        geometry.attachChild(it)
        it.setLocalTranslation(3f, 0f, 0f)
        it.localRotation = Quaternion().fromAngles(0f, 1.57f, 0f)
    }

    private var isMovingIn = false
    private var strokeHandCtrl = NumberSmoother(0f, 10.0)
    private var handState: HandState = HandState.Resting
    private var restingHandLocCtrl = VectorSmoother(HandState.Resting.translation, 30.0)
    private var restingHandRotCtrl = VectorSmoother(HandState.Resting.rotation, 30.0)

    init {
        with(geometry) {
            loc = v3(-40, 15, -20)
            rot = v3(90, -65, 0)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        muteCollector.advanceCollectOne(time)?.let { onPlay(HandState.Resting) }
        openCollector.advanceCollectOne(time)?.let { onPlay(HandState.Lifted) }
        strokeHand.setLocalTranslation(0f, strokeHandCtrl.tick(delta) { if (isMovingIn) 1f else -1f }, 0f)
        with(restHand) {
            loc = restingHandLocCtrl.tick(delta) { handState.translation }
            rot = restingHandRotCtrl.tick(delta) { handState.rotation }
        }
    }

    private fun onPlay(handState: HandState) {
        isMovingIn = !isMovingIn
        this@Cuica.handState = handState
    }

    private sealed class HandState(val translation: Vector3f, val rotation: Vector3f) {
        data object Lifted : HandState(v3(3, 1, 0), v3(15, 90, 0))
        data object Resting : HandState(v3(3, 0, 0), v3(0, 90, 0))
    }
}
