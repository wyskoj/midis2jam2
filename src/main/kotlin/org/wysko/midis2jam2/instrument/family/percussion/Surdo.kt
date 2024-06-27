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

import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.Surdo.HandPosition.Down
import org.wysko.midis2jam2.instrument.family.percussion.Surdo.HandPosition.Up
import org.wysko.midis2jam2.util.VectorSmoother
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/** The Surdo. */
class Surdo(
    context: Midis2jam2,
    muteHits: MutableList<NoteEvent.NoteOn>,
    openHits: MutableList<NoteEvent.NoteOn>,
) : AuxiliaryPercussion(context, (muteHits + openHits).sortedBy { it.tick }.toMutableList()) {
    private val muteCollector = EventCollector(context, muteHits)
    private val openCollector = EventCollector(context, openHits)

    private val stick =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = StickType.DRUM_SET_STICK,
        ).apply {
            setParent(recoilNode)
            offsetStick { it.move(0f, 0f, -2f) }
            node.move(0f, 0f, 14f)
        }

    /** The hand that rests or hovers above the drum. */
    private val hand: Spatial =
        context.modelD("hand_left.obj", "hands.bmp").also {
            recoilNode.attachChild(it)
        }

    private var handPosition: HandPosition = Up
    private val handLocCtrl = VectorSmoother(Up.translation, 20.0)
    private val handRotCtrl = VectorSmoother(Up.rotation, 20.0)

    init {
        recoilNode.attachChild(
            context.modelD("DrumSet_Surdo.obj", "DrumShell_Surdo.png").apply {
                setLocalScale(1.7f)
            },
        )
        with(placement) {
            loc = v3(25, 25, -41)
            rot = v3(14.2, -90, 0)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        val results = stick.tick(time, delta)
        recoilDrum(recoilNode, results.velocity, delta)

        muteCollector.advanceCollectOne(time)?.let { handPosition = Down }
        openCollector.advanceCollectOne(time)?.let { handPosition = Up }

        hand.loc = handLocCtrl.tick(delta) { handPosition.translation }
        hand.rot = handRotCtrl.tick(delta) { handPosition.rotation }
    }

    override fun onEntry() {
        val nextMute = muteCollector.peek()
        val nextOpen = openCollector.peek()

        val handPosition = when {
            nextMute == null && nextOpen != null -> Up
            nextMute != null && nextOpen == null -> Down
            nextMute != null && nextOpen != null -> if (nextMute.tick < nextOpen.tick) Down else Up
            else -> error("All cases covered.")
        }

        this.handPosition = handPosition
        handLocCtrl.value = handPosition.translation.clone()
        handRotCtrl.value = handPosition.rotation.clone()
    }

    private sealed class HandPosition(val translation: Vector3f, val rotation: Vector3f) {
        data object Up : HandPosition(v3(0, 2, 0), v3(30, 0, 0))
        data object Down : HandPosition(v3(0, 0, 0), v3(0, 0, 0))
    }
}
