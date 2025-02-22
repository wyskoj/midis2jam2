package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.FastMath.PI
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.SpatialPool
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.modelD
import org.wysko.midis2jam2.application.modelR
import org.wysko.midis2jam2.collector.EventCollector
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.common.GlowControl
import org.wysko.midis2jam2.instrument.common.OscillatorControl
import org.wysko.midis2jam2.instrument.common.invokeHitControls
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.midi.pitchClass
import org.wysko.midis2jam2.seconds
import kotlin.time.Duration

class MusicBox(private val context: PerformanceAppState, events: List<MidiEvent>) : DecayedInstrument(context, events) {
    private val lamellae = List(12) {
        context.modelR("music_box-lamella.obj", "silver.png").apply {
            loc = vec3(it - 5.5, 7, 0)
            scaleVec = vec3(-0.04545f * it + 1, 1, 1)
            addControl(OscillatorControl(
                amplitude = 1f,
                frequency = 3f,
                phaseAngle = PI / 2,
                qFactor = 2.5f
            ))
            addControl(GlowControl())
        }
    }.onEach { root += it }

    private val pointCollector = EventCollector(context, hits, { event, time ->
        context.sequence.getTimeAtTick(event.tick - context.sequence.smf.tpq) <= time // Quarter note early
    })
    private val pointPool = SpatialPool(context.modelR("music_box-point.obj", "silver.png"))

    init {
        root += context.modelD("music_box-case.obj", "wood.png")
        root += context.modelR("music_box-top_blade.obj", "silver.png")
        context.modelR("music_box-spindle.obj", "silver.png").apply {
            addControl(TempoAwareRotationControl())
        }.also { root += it }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        pointCollector.advanceCollectAll(time).forEach { note ->
            pointPool.obtain().also {
                if (it.getControl(TempoAwareRotationControl::class.java) == null) {
                    it.addControl(TempoAwareRotationControl())
                }
            }.also {
                it.loc = vec3((note.note + 3) % 12 - 5.5, 0, 0)
                it.rot = vec3(-90, 0, 0)
                root += it
            }
        }
        pointPool.inUse.filter { it.rot.x > 135 }.forEach {
            root -= it
            pointPool.free(it)
        }
    }

    override fun onHit(noteOn: NoteEvent.NoteOn) {
        super.onHit(noteOn)
        lamellae[pitchClass(noteOn.note)].invokeHitControls(noteOn.velocity)
    }

    private inner class TempoAwareRotationControl : AbstractControl() {
        override fun controlUpdate(tpf: Float) {
            val tempo = context.sequence.getTempoAtTime(context.time.seconds).beatsPerMinute.toFloat()
            spatial.rotate(PI * tpf * tempo / 120.0f, 0f, 0f)
        }

        override fun controlRender(rm: RenderManager?, vp: ViewPort?) = Unit
    }
}