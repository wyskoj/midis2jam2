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
import org.wysko.midis2jam2.instrument.algorithmic.StrikeWeight
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletKeyframe
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletPath
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.Point3
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.DrumStickPlanner
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.DrumTarget
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.HandProfile
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.StickHit
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.StickTravel
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.sharedHits
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
        val shared = sharedHits(plan.perHand, SHARED_WINDOW)

        sticks = plan.perHand.mapIndexed { hand, handHits ->
            val positions = handHits.mapIndexed { i, hit ->
                val pose = poses.getValue(hit.target.id)
                if (shared[hand][i]) {
                    pose.translation.add(sideways(pose.rotation, hand)).toPoint3()
                } else {
                    hit.target.position
                }
            }
            RoamingStick(context, kit, handHits, positions, handHits.map { poses.getValue(it.target.id).rotation })
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
        positions: List<Point3>,
        private val rotations: List<Quaternion>,
    ) {
        private val targetIds = hits.associate { it.source to it.target.id }

        private val striker = Striker(
            context,
            hits.map { it.source },
            StickType.DRUM_SET_STICK,
            windUpRise = WIND_UP_RISE,
            weight = { targetIds[it]?.let(::weightFor) ?: StrikeWeight.NORMAL },
            stickyGap = STICKY_GAP,
        ).apply {
            setParent(kit)
        }

        private val keyframes = hits.zip(positions) { hit, position -> MalletKeyframe(hit.time, position) }

        private val path = MalletPath(keyframes)

        private val travel = StickTravel(keyframes, TRAVEL_SPEED, MIN_TRAVEL)

        val node: Node get() = striker.node

        init {
            place(0.0)
        }

        fun tick(time: Duration, delta: Duration) {
            place(travel.pathTime(time.toDouble(SECONDS)))
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

        /** How the sticks strike most of the kit: with a deeper swing than usual, so the hits land with weight. */
        val HEAVY = StrikeWeight(backswing = 1.6)

        /** How the sticks strike a crash, hardest of all: swung right back, lifted high, and wound up for longer. */
        val CRASH = StrikeWeight(backswing = 2.6, rise = 1.8, windUp = 1.5)

        /** How a stick strikes the target [id]. The ride and hi-hat are played gently, as usual. */
        fun weightFor(id: String): StrikeWeight = when {
            id.startsWith("ride") || id == "hi_hat" -> StrikeWeight.NORMAL
            id.startsWith("cymbal_crash") -> CRASH
            else -> HEAVY
        }

        /**
         * The longest gap between strikes, in seconds, that a stick stays out through. Longer than a piece's own
         * stick, since a roaming stick waits on the piece it struck rather than drifting off.
         */
        const val STICKY_GAP = 4.0

        /** How fast a stick travels between pieces, in the kit's units per second (the snare to the hi-hat is ~10). */
        const val TRAVEL_SPEED = 20.0

        /** The least time a stick takes to travel between pieces, in seconds. */
        const val MIN_TRAVEL = 0.4

        /** How close together in time both hands strike a piece for them to share it and move aside, in seconds. */
        const val SHARED_WINDOW = 0.5

        /** How far each stick moves aside on a piece both hands share, in the kit's units. */
        const val SHARED_SPACING = 1.0f

        /**
         * Which way, and how far, [hand]'s stick moves aside on a piece both hands share: along the axis the stick
         * swings about (so the sticks end up side by side), the left hand to the drummer's left (the kit's -X, where
         * the hi-hat is) and the right hand to the right.
         */
        fun sideways(rotation: Quaternion, hand: Int): Vector3f {
            val axis = rotation.mult(Vector3f.UNIT_X)
            if (axis.x < 0) axis.negateLocal()
            return axis.multLocal(if (hand == HandProfile.LEFT) -SHARED_SPACING else SHARED_SPACING)
        }

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
