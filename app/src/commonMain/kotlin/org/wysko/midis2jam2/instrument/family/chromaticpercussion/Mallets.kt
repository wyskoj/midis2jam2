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
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.MAX_STICK_IDLE_ANGLE
import org.wysko.midis2jam2.instrument.algorithmic.StickStatus
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.BarState.DOWN
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.BarState.UP
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletChoreographer
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletHit
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletKeyframe
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletPath
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletPlanner
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.Point3
import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.White
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.DIM_GLOW
import org.wysko.midis2jam2.world.assetLoader
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private const val MALLET_CASE_SCALE: Float = 0.6666667f
private val RANGE = 21..108

/** How far apart side-by-side roaming mallets strike, so two on the same bar don't overlap. */
private const val MALLET_SPREAD = 0.35f

/** Where a mallet's shadow falls, relative to the mallet. */
private val SHADOW_OFFSET = Vector3f(0f, -0.6f, -8f)

/**
 * The mallet instruments.
 *
 * By default every bar has its own mallet. With the smart mallets setting on, [malletCount] mallets instead travel
 * between the bars, the way a player's hands do. [MalletPlanner] decides which mallet strikes each note, and
 * [MalletChoreographer] times their moves so they keep out of each other's way.
 */
class Mallets(
    context: PerformanceManager,
    eventList: List<MidiEvent>,
    private val type: MalletType,
    private val malletCount: Int = 2,
) : DecayedInstrument(context, eventList) {

    private val isSmart = context.config.settings.instrumentSettings.isSmartMallets

    private val hitsByNote: List<List<NoteEvent.NoteOn>> = RANGE.map { x -> _hits.filter { it.note.toInt() == x } }

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
                val events = hitsByNote[index].takeUnless { isSmart }
                if (Key.Color.fromNoteNumber(byte) == White) {
                    MalletBar(byte, whiteCount++, events)
                } else {
                    MalletBar(byte, note, events)
                }
            }
        }.onEach { geometry += it.root }

    private val roamingMallets: List<RoamingMallet>

    /** Notes no roaming mallet strikes (chords bigger than the number of mallets); their bars recoil on their own. */
    private val unassignedHits: EventCollector<NoteEvent.NoteOn>?

    init {
        placement.loc = v3(18, 0, -5)
        with(geometry) {
            +context.modelD("XylophoneCase.obj", "Black.bmp").apply {
                setLocalScale(MALLET_CASE_SCALE)
            }
        }

        if (isSmart) {
            val plan = MalletPlanner.plan(
                hits = _hits.filter { it.note.toInt() in RANGE }.map {
                    MalletHit(context.sequence.getTimeOf(it).toDouble(SECONDS), it.note.toInt(), it)
                },
                malletCount = malletCount,
                x = { barFor(it).strikePoint.x.toDouble() },
            )
            val keyframes = plan.perMallet.mapIndexed { i, hits ->
                val spread = (i - (malletCount - 1) / 2f) * MALLET_SPREAD
                hits.map { hit ->
                    val point = barFor(hit.note).strikePoint
                    MalletKeyframe(hit.time, Point3(point.x + spread.toDouble(), point.y.toDouble(), point.z.toDouble()))
                }
            }
            val paths = MalletChoreographer.choreograph(keyframes)
            roamingMallets = plan.perMallet.mapIndexed { i, hits -> RoamingMallet(hits, paths[i]) }
            unassignedHits = EventCollector(context, plan.unassigned.map { it.source })
        } else {
            roamingMallets = emptyList()
            unassignedHits = null
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

        roamingMallets.forEach { it.tick(time, delta) }
        unassignedHits?.advanceCollectAll(time)?.forEach { barFor(it.note.toInt()).recoil() }
        bars.forEach { it.tick(time, delta) }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        val index = updateInstrumentIndex(delta) - 2
        geometry.loc = v3(-53, 26.5 + (2 * index), 0)
        placement.rot = v3(0, -18 * index, 0)
    }

    private fun barFor(note: Int): MalletBar = bars[note - RANGE.first]

    /** The roaming mallets' nodes, left to right, in geometry space. Empty unless smart mallets are on. */
    internal val roamingMalletNodes: List<Node> get() = roamingMallets.map { it.node }

    /** Where a mallet strikes [note]'s bar, in geometry space. */
    internal fun strikePointOf(note: Int): Vector3f = barFor(note).strikePoint.clone()

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
     * A mallet that travels between the bars, striking the notes [MalletPlanner] gave it.
     *
     * @param hits the notes this mallet strikes, in time order.
     * @param path where the mallet is over time, from [MalletChoreographer].
     */
    private inner class RoamingMallet(hits: List<MalletHit<NoteEvent.NoteOn>>, private val path: MalletPath) {

        private val striker = Striker(
            context,
            hits.map { it.source },
            context.modelD("XylophoneMalletWhite.obj", type.textureFile),
        ).apply {
            // Changes the pivot point of rotation
            offsetStick { it.move(0f, 0f, -2f) }
            node.scale(MALLET_CASE_SCALE)
        }

        val node: Node get() = striker.node

        private val shadow: Spatial = context.modelD("MalletHitShadow.obj", "Black.bmp").apply { scale(0f) }

        init {
            striker.node.loc = path.positionAt(0.0).toVector3f()
            geometry += striker.node
            geometry += shadow
        }

        fun tick(time: Duration, delta: Duration) {
            val seconds = time.toDouble(SECONDS)
            striker.node.loc = path.positionAt(seconds).toVector3f()
            shadow.loc = path.positionAt(seconds, lifted = false).toVector3f().addLocal(SHADOW_OFFSET)

            val status = striker.tick(time, delta)
            status.strike?.let { barFor(it.note.toInt()).recoil() }
            shadow.setLocalScale(shadowScale(status))
        }

        private fun Point3.toVector3f() = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
    }

    /**
     * Represents a single bar on the mallet instrument.
     *
     * @param events the notes this bar's own mallet strikes, or `null` if roaming mallets play this bar instead.
     */
    inner class MalletBar(midiNote: Byte, startPos: Int, events: List<NoteEvent.NoteOn>?) {

        internal val root = Node()
        private val bar = with(root) {
            +node()
        }
        private var upBar: Spatial
        private var downBar: Spatial
        private val shadow: Spatial? = events?.let { context.modelD("MalletHitShadow.obj", "Black.bmp") }

        private var isRecoiling = false
        private var recoilNow = false

        private val mallet = events?.let {
            Striker(
                context,
                it,
                context.modelD("XylophoneMalletWhite.obj", type.textureFile),
                sticky = false,
            ).apply {
                // Changes the pivot point of rotation
                offsetStick { stick -> stick.move(0f, 0f, -2f) }
                node.apply {
                    move(0f, 0f, 2f)
                    scale(MALLET_CASE_SCALE)
                }
            }
        }

        /** Where a mallet strikes this bar, in the instrument's geometry space. */
        val strikePoint: Vector3f

        init {
            val scaleFactor = (RANGE.last - midiNote + 20) / 50f

            if (Key.Color.fromNoteNumber(midiNote) == White) {
                upBar = context.modelD("XylophoneWhiteBar.obj", type.textureFile)
                downBar = context.modelD("XylophoneWhiteBarDown.obj", type.textureFile)
                    .apply { (this as Geometry).material.setColor("GlowColor", DIM_GLOW) }

                bar.setLocalScale(0.55f, 1f, 0.5f * scaleFactor)
                root.loc = v3(1.333f * (startPos - 26), 0f, 0f)
                strikePoint = v3(0f, 1.35f, -midiNote / 11.5f + 19)
            } else {
                upBar = context.modelD("XylophoneBlackBar.obj", type.textureFile)
                downBar = context.modelD("XylophoneBlackBarDown.obj", type.textureFile)
                    .apply { (this as Geometry).material.setColor("GlowColor", DIM_GLOW) }

                bar.setLocalScale(0.6f, 0.7f, 0.5f * scaleFactor)
                root.move(1.333f * (midiNote * 0.583f - 38.2f), 0f, -midiNote / 50f + 2.667f)
                strikePoint = v3(0f, 2.6f, midiNote / 12.5f - 2)
            }
            mallet?.node?.loc = strikePoint.clone()
            shadow?.loc = strikePoint.add(SHADOW_OFFSET)
            strikePoint.addLocal(root.localTranslation)

            with(root) {
                +bar.apply {
                    +downBar.apply { cullHint = false.ch }
                    +upBar
                }
                mallet?.let { +it.node }
                shadow?.let { +it.apply { scale(0f) } }
            }
        }

        /**
         * Animates the mallet and the bar.
         *
         * @param time the current time
         * @param delta the amount of time since the last frame update
         */
        fun tick(time: Duration, delta: Duration) {
            mallet?.tick(time, delta)?.let {
                if (it.velocity > 0) recoil()
                shadow?.setLocalScale(shadowScale(it))
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

        /** Knocks the bar down, to spring back up over the next few frames. */
        fun recoil() {
            isRecoiling = true
            recoilNow = true
        }

        private fun showBar(barState: BarState) {
            upBar.cullHint = (barState == UP).ch
            downBar.cullHint = (barState == DOWN).ch
        }
    }
}

/** How big a mallet's shadow is, given how far the mallet has swung up from the bar. */
private fun shadowScale(status: StickStatus): Float =
    ((1 - Math.toDegrees(status.rotationAngle.toDouble()) / MAX_STICK_IDLE_ANGLE) / 2).toFloat().coerceAtLeast(0.0f)

private enum class BarState {
    UP, DOWN
}
