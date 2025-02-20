package org.wysko.midis2jam2.instrument.family.organ

import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.modelD
import org.wysko.midis2jam2.dSeconds
import org.wysko.midis2jam2.instrument.KeyedInstrument
import org.wysko.midis2jam2.instrument.family.piano.KeyControl
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.midi.NoteColor
import org.wysko.midis2jam2.midi.noteNumberToPitch
import org.wysko.midis2jam2.scene.Axis
import kotlin.time.Duration

private val PLAYING_RANGE = 0..<24
private val SQUEEZE_RANGE = 1.0..4.0

class Accordion(
    context: PerformanceAppState,
    events: List<MidiEvent>,
    variant: Variant = Variant.Accordion,
) : KeyedInstrument(context, events, PLAYING_RANGE) {

    private var state = State.Contracting
    private var angle = SQUEEZE_RANGE.endInclusive
    private var angularVelocity = 0.0f

    override val keys: List<Spatial> = List(24) { i ->
        context.modelD(
            model = "accordion/key-${noteNumberToPitch(i)}.obj",
            texture = if (NoteColor.fromNoteNumber(i) == NoteColor.White) "accordion/key-white.png" else "accordion/key-black.png"
        ).apply { addControl(KeyControl(rotationAxis = Axis.Y, invertRotation = true)) }.also {
            it.loc.y = -(i + PLAYING_RANGE.first) / 12 * 7f
        }
    }


    init {
        repeat(14) {
            node {
                when (it) {
                    0 -> this += context.modelD("accordion/case-left.j3o").also {
                        ((it as Node).children[2] as Geometry).material.setFloat("HueShift", variant.hue)
                    }

                    13 -> {
                        this += context.modelD("accordion/case-right.obj", "accordion/case-front.png").also {
                            (it as Geometry).material.setFloat("HueShift", variant.hue)
                        }
                        this += node {
                            keys.forEach { key ->
                                this += key
                            }

                            this += context.modelD("accordion/key-white.obj", "accordion/key-white.png").also {
                                it.loc = vec3(0, 4, 0)
                            }
                            this += context.modelD("accordion/key-white.obj", "accordion/key-white.png").also {
                                it.loc = vec3(0, -11, 0)
                            }
                            this.loc = vec3(-4, 25, -0.8)
                        }
                    }

                    else -> this += context.modelD("accordion/fold.obj", "accordion/fold.png")
                }
            }.apply {
                addControl(SectionControl(it))
            }.also {
                root += it
            }
        }
    }

    override fun keyFromNoteNumber(noteNumber: Int): Spatial? = keys.getOrNull(noteNumber % 24)

    override fun isNotePlaying(noteNumber: Int): TimedArc? =
        collector.currentArcs.find { (it.note.toInt() % 24) == noteNumber }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        angularVelocity = when {
            collector.currentArcs.isEmpty() -> interpTo(angularVelocity, 0, delta.dSeconds, 3)
            else -> 2f
        }

        when {
            angle < SQUEEZE_RANGE.start -> State.Expanding
            angle > SQUEEZE_RANGE.endInclusive -> State.Contracting
            else -> null
        }?.let { state = it }

        angle += delta.dSeconds * angularVelocity * state.sign
    }

    enum class Variant(internal val hue: Float) {
        Accordion(266 / 360f), TangoAccordion(28 / 360f),
    }

    private inner class SectionControl(private val index: Int) : AbstractControl() {
        override fun controlUpdate(tpf: Float) {
            spatial.rot = vec3(0, 0, angle * (index - 7.5f))
        }

        override fun setSpatial(spatial: Spatial?) {
            super.setSpatial(spatial)
            spatial?.rot = vec3(0, 0, SQUEEZE_RANGE.endInclusive * (index - 7.5f))
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
    }

    private enum class State {
        Contracting, Expanding;

        val sign: Int
            get() = when (this) {
                Contracting -> -1
                Expanding -> 1
            }
    }
}