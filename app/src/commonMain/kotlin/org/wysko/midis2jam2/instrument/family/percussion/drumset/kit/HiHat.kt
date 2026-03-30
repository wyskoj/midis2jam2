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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.kit

import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSetInstrument
import org.wysko.midis2jam2.util.max
import org.wysko.midis2jam2.world.model
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val AMPLITUDE = 0.25
private const val DAMPENING = 2.0
private const val WOBBLE_SPEED = 10.0
private val MAX_PEDAL_WINDOW = 200.milliseconds
private val MAX_OPEN_WINDOW = 200.milliseconds
private val MAX_CLOSE_WINDOW = 50.milliseconds

/** The hi-hat. */
class HiHat(
    context: PerformanceManager,
    hits: List<NoteEvent.NoteOn>,
    private val noteMapping: HiHatNoteMapping = HiHatNoteMapping.Standard,
    style: Cymbal.Style = Cymbal.Style.Standard,
) : DrumSetInstrument(context, hits) {
    private val cymbalsNode =
        Node().apply {
            recoilNode.attachChild(this)
            scale(1.3f)
            move(0f, 0f, -13f)
        }

    private val topCymbal: Spatial =
        context.model(
            model = "DrumSet_Cymbal.obj",
            texture = style.texture,
            type = style.materialType,
        ).apply {
            localTranslation.set(HiHatState.Closed.position)
            cymbalsNode.attachChild(this)
        }

    init {
        // Add bottom cymbal
        context.model(
            model = "DrumSet_Cymbal.obj",
            texture = style.texture,
            type = style.materialType,
        ).apply {
            rotate(FastMath.PI, 0f, 0f) // Rotate upside down
            cymbalsNode.attachChild(this)
        }
    }

    private val stick: Striker =
        Striker(
            context = context,
            strikeEvents = hits.filter { it.note != noteMapping.pedal },
            stickModel = StickType.DRUM_SET_STICK,
        ).apply {
            setParent(recoilNode)
            node.move(0f, 1f, 2f)
        }

    private val cymbalAnimator = CymbalAnimator(topCymbal, AMPLITUDE, WOBBLE_SPEED, DAMPENING)

    private val pedalEventCollector =
        EventCollector(
            context = context,
            events = hits.toList().filter { it.note == noteMapping.pedal },
        )

    private var state = HiHatState.Closed

    init {
        geometry.move(-6f, 22f, -72f)
        geometry.rotate(0f, 1.57f, 0f)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        val stickResults = stick.tick(time, delta)
        stickResults.strike?.let {
            cymbalAnimator.strike()
            when (it.note) {
                noteMapping.open -> setState(HiHatState.Open)
                else -> setState(HiHatState.Closed)
            }
        }

        val pedalResults = pedalEventCollector.advanceCollectOne(time)
        pedalResults?.let { setState(HiHatState.Closed) }

        when (val peek = peekType()) {
            EventType.Pedal -> pedalEventCollector.peek()?.let { peek ->
                val peekTime = context.sequence.getTimeOf(peek)
                val timeToNextPedal = (peekTime - time)
                val window = (peekTime - timeOfLastEvent()).coerceAtMost(MAX_PEDAL_WINDOW)
                val cursor = (1 - (timeToNextPedal.coerceAtMost(window) / window).toFloat()).let {
                    when (state) {
                        HiHatState.Closed -> it
                        HiHatState.Open -> it.coerceIn(0.5f..1f)
                    }
                }
                val scaleFactor = sqrt(window / MAX_PEDAL_WINDOW).toFloat()
                topCymbal.localTranslation.interpolateLocal(
                    HiHatState.Closed.position,
                    HiHatState.Open.position,
                    sin(cursor * FastMath.PI) * scaleFactor
                )
            }

            EventType.Closed -> animateHatTransition(time, eventType = peek, MAX_CLOSE_WINDOW)
            EventType.Open -> animateHatTransition(time, eventType = peek, MAX_OPEN_WINDOW)
            else -> Unit
        }

        recoilDrum(
            drum = recoilNode,
            velocity = max(stickResults.strike?.velocity ?: 0, pedalResults?.velocity ?: 0),
            delta = delta,
            recoilDistance = -0.7f,
            recoilSpeed = 12f,
        )

        cymbalAnimator.tick(delta)
    }

    private fun timeOfLastEvent(): Duration {
        val lastPedal = pedalEventCollector.prev()?.let { context.sequence.getTimeOf(it) } ?: (-2.0).seconds
        val lastStrike = stick.prev()?.let { context.sequence.getTimeOf(it) } ?: (-2.0).seconds
        return maxOf(lastPedal, lastStrike)
    }

    private fun peekType(): EventType? {
        val peekPedal = pedalEventCollector.peek()
        val peekStick = stick.peek()
        return when {
            peekPedal != null && peekStick != null -> {
                val peekPedalTime = context.sequence.getTimeOf(peekPedal)
                val peekStickTime = context.sequence.getTimeOf(peekStick)
                if (peekPedalTime < peekStickTime) EventType.Pedal else noteMapping.eventTypeByNote(peekStick.note)
            }

            peekPedal != null -> EventType.Pedal
            peekStick != null -> noteMapping.eventTypeByNote(peekStick.note)
            else -> null
        }
    }

    private fun setState(state: HiHatState) {
        this.state = state
        this.topCymbal.localTranslation.set(state.position)
        if (state == HiHatState.Closed) {
            cymbalAnimator.cancel()
        }
    }

    private fun animateHatTransition(time: Duration, eventType: EventType, window: Duration) {
        val (triggerState, from, to) = when (eventType) {
            EventType.Closed -> Triple(HiHatState.Open, HiHatState.Open.position, HiHatState.Closed.position)
            EventType.Open -> Triple(HiHatState.Closed, HiHatState.Closed.position, HiHatState.Open.position)
            else -> return
        }
        if (state != triggerState) return

        val peekTime = context.sequence.getTimeOf(stick.peek() ?: return)
        val timeToNext = (peekTime - time)
        val window = (peekTime - timeOfLastEvent()).coerceAtMost(window)
        val cursor = (timeToNext.coerceAtMost(window) / window).toFloat()
        topCymbal.localTranslation.interpolateLocal(
            from,
            to,
            if (cursor < 0.5f) 1.0f else sin(cursor * FastMath.PI)
        )
    }

    enum class EventType {
        Open, Closed, Pedal
    }
}


/**
 * The hi-hat note mapping.
 *
 * @property closed The MIDI note for the closed hi-hat.
 * @property open The MIDI note for the open hi-hat.
 * @property pedal The MIDI note for the hi-hat pedal.
 */
sealed class HiHatNoteMapping(
    val closed: Byte,
    val open: Byte,
    val pedal: Byte,
) {
    /** The mapping as seen in most all kits. */
    data object Standard : HiHatNoteMapping(
        closed = 42,
        open = 46,
        pedal = 44,
    )

    /** The mapping as seen in the orchestra kit. */
    data object Orchestra : HiHatNoteMapping(
        closed = 27,
        open = 29,
        pedal = 28,
    )

    fun eventTypeByNote(note: Byte): HiHat.EventType {
        return when (note) {
            closed -> HiHat.EventType.Closed
            open -> HiHat.EventType.Open
            pedal -> HiHat.EventType.Pedal
            else -> throw IllegalArgumentException("Note $note does not correspond to any hi-hat event type.")
        }
    }
}

private enum class HiHatState(
    val position: Vector3f
) {
    Open(Vector3f(0f, 2f, 0f)),
    Closed(Vector3f(0f, 1.2f, 0f));
}