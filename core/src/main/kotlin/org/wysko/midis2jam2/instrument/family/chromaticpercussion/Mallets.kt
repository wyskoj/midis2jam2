package org.wysko.midis2jam2.instrument.family.chromaticpercussion

import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.application.PerformanceAppState
import org.wysko.midis2jam2.application.model
import org.wysko.midis2jam2.instrument.DecayedInstrument
import org.wysko.midis2jam2.instrument.common.Striker
import org.wysko.midis2jam2.instrument.common.Striker.Companion.makeStriker
import org.wysko.midis2jam2.jme3ktdsl.*
import org.wysko.midis2jam2.midi.NoteColor
import org.wysko.midis2jam2.midi.whiteIndexFromNoteNumber

private val RANGE = 21..108

class Mallets(
    context: PerformanceAppState,
    events: List<MidiEvent>,
    variant: Variant,
) : DecayedInstrument(context, events) {

    private val bars: List<Spatial> = RANGE.map { noteNumber ->
        node {
            this += context.model(
                model = when (NoteColor.fromNoteNumber(noteNumber)) {
                    NoteColor.White -> "mallets-bar-white.obj"
                    NoteColor.Black -> "mallets-bar-black.obj"
                },
                texture = variant.texture
            )
        }.apply {
            val scaleFactor = (RANGE.last - noteNumber + 20) / 100f

            when (NoteColor.fromNoteNumber(noteNumber)) {
                NoteColor.White -> {
                    loc = vec3(
                        x = (4 / 3.0) * (whiteIndexFromNoteNumber(noteNumber) - 38),
                        y = 0,
                        z = 0
                    )
                    scaleVec = vec3(0.55, 1, scaleFactor)
                }

                NoteColor.Black -> {
                    loc = vec3(
                        x = (4 / 3.0) * (noteNumber * 0.583 - 38.2),
                        y = 0,
                        z = -noteNumber / 50.0 + (8 / 3.0)
                    )
                    scaleVec = vec3(0.6, 0.7, scaleFactor)
                }
            }
            addControl(BarControl())
        }.also { root += it }
    }

    init {
        RANGE.mapIndexed { index, noteNumber ->
            context.makeStriker(
                hits = hits.filter { it.note.toInt() == noteNumber },
                variant = object : Striker.Variant() {
                    override val model: String = "mallet.obj"
                    override val texture: String = variant.texture
                },
                parameters = Striker.Parameters(visibilityBehavior = Striker.VisibilityBehavior.OnlyNecessary),
                onStrike = { velocity ->
                    bars[index].getControl(BarControl::class.java).hit(velocity)
                }
            ).also {
                root += it
                it.loc = when (NoteColor.fromNoteNumber(noteNumber)) {
                    NoteColor.White -> vec3(
                        x = (4 / 3.0) * (whiteIndexFromNoteNumber(noteNumber) - 38),
                        y = 1.35,
                        z = -noteNumber / 9.5 + 19
                    )

                    NoteColor.Black -> vec3(
                        x = 0.777 * noteNumber - 50.933,
                        y = 2.6,
                        z = 0.045 * noteNumber + 0.667
                    )
                }
                it.scale = 2 / 3.0
            }
        }
        root += context.model("mallets-case.obj", ColorRGBA.Black).apply { scale = 2 / 3.0 }
    }

    enum class Variant(internal val texture: String) {
        Glockenspiel("mallets-glockenspiel.png"),
        Marimba("mallets-marimba.png"),
        Vibraphone("mallets-vibraphone.png"),
        Xylophone("mallets-xylophone.png"),
    }

    private class BarControl : AbstractControl() {
        fun hit(velocity: Int) {
            getBar().loc = vec3(0, -1 * velocityRamp(velocity), 0)
        }

        override fun controlUpdate(tpf: Float) {
            with(getBar()) {
                loc.interpolateLocal(Vector3f.ZERO, tpf * 10f)
                material.setFloat("AOIntensity", 1 - localTranslation.y)
            }
        }

        private fun getBar(): Geometry = ((spatial as Node).children.first() as Geometry)

        override fun controlRender(rm: RenderManager, vp: ViewPort) = Unit
    }
}
