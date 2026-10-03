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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.math.Quaternion
import com.jme3.math.Transform
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletKeyframe
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletPath
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.Point3
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.DrumStickPlanner
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.DrumTarget
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.StickHit
import org.wysko.midis2jam2.manager.PerformanceManager
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * The smart drum sticks: two sticks, held by a right-handed drummer playing crossed, that travel around the kit
 * instead of every piece having a stick of its own.
 *
 * Each piece keeps its own stick, hidden (a "ghost"), which still strikes on time so the piece still reacts; the ghost
 * also says where on the piece a stick goes. [DrumStickPlanner] decides which hand strikes each hit, and each stick
 * follows a [MalletPath] through its strikes, turning to match each piece's stick angle as it goes.
 *
 * @param kit the kit's geometry node, which [pieces] are attached under and the sticks are added to.
 */
class SmartDrumSticks(
    context: PerformanceManager,
    kit: Node,
    pieces: List<DrumSetInstrument>,
) {
    private val sticks: List<RoamingStick>

    /** Where the sticks strike each target, by target id, in the kit's space. */
    val targetPositions: Map<String, Vector3f>

    init {
        val targets = pieces.flatMap { piece -> piece.stickTargets().map { piece to it } }
        val poses = targets.associate { (_, target) -> target.id to poseInKit(target.pose, kit) }
        targetPositions = poses.mapValues { it.value.translation.clone() }
        val drumTargets = targets.associate { (_, target) ->
            target.id to DrumTarget(target.id, poses.getValue(target.id).translation.toPoint3(), target.profile)
        }

        val hits = targets.flatMap { (piece, target) ->
            piece.hits.filter { target.plays(it.note) }.map {
                StickHit(context.sequence.getTimeOf(it).toDouble(SECONDS), drumTargets.getValue(target.id), it)
            }
        }

        // The left hand starts over the snare, the right over the hi-hat.
        val fallback = drumTargets.values.firstOrNull()?.position ?: Point3(0.0, 0.0, 0.0)
        val home = listOf(
            drumTargets["snare"]?.position ?: fallback,
            drumTargets["hi_hat"]?.position ?: fallback,
        )
        val plan = DrumStickPlanner.plan(hits, home)

        sticks = plan.perHand.map { handHits ->
            RoamingStick(context, kit, handHits, handHits.map { poses.getValue(it.target.id).rotation })
        }
    }

    /** The sticks' nodes, left hand first, in the kit's space. */
    val nodes: List<Node> get() = sticks.map { it.node }

    /** Moves the sticks and swings them, given the current [time] and the time since the last frame ([delta]). */
    fun tick(time: Duration, delta: Duration) {
        sticks.forEach { it.tick(time, delta) }
    }

    private class RoamingStick(
        context: PerformanceManager,
        kit: Node,
        hits: List<StickHit<NoteEvent.NoteOn>>,
        private val rotations: List<Quaternion>,
    ) {
        private val striker = Striker(context, hits.map { it.source }, StickType.DRUM_SET_STICK, windUpRise = WIND_UP_RISE).apply {
            setParent(kit)
        }

        private val path = MalletPath(hits.map { MalletKeyframe(it.time, it.target.position) })

        val node: Node get() = striker.node

        init {
            place(0.0)
        }

        fun tick(time: Duration, delta: Duration) {
            place(time.toDouble(SECONDS))
            striker.tick(time, delta)
        }

        private fun place(seconds: Double) {
            if (rotations.isEmpty()) return
            val position = path.positionAt(seconds)
            node.setLocalTranslation(position.x.toFloat(), position.y.toFloat(), position.z.toFloat())

            val progress = path.segmentProgressAt(seconds)
            node.localRotation = when {
                progress != null -> Quaternion().apply {
                    slerp(rotations[progress.first], rotations[progress.first + 1], progress.second.toFloat())
                }

                seconds <= path.keyframes.first().time -> rotations.first()
                else -> rotations.last()
            }
        }
    }

    private companion object {
        /** How high the sticks rise as they wind up to a strike, in the kit's units (on top of their swing). */
        const val WIND_UP_RISE = 3.0

        /** Where [pose] sits relative to [kit], from the local transforms between them. */
        fun poseInKit(pose: Spatial, kit: Node): Transform {
            val transform = pose.localTransform.clone()
            var parent = pose.parent
            while (parent != null && parent !== kit) {
                transform.combineWithParent(parent.localTransform)
                parent = parent.parent
            }
            check(parent === kit) { "A stick target isn't attached under the kit" }
            return transform
        }

        fun Vector3f.toPoint3() = Point3(x.toDouble(), y.toDouble(), z.toDouble())
    }
}
