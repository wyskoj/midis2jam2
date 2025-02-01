package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.scene.Geometry
import org.wysko.midis2jam2.dSeconds
import org.wysko.midis2jam2.instrument.KeyedInstrument
import org.wysko.midis2jam2.interpTo
import org.wysko.midis2jam2.jme3ktdsl.node
import org.wysko.midis2jam2.jme3ktdsl.plusAssign
import org.wysko.midis2jam2.jme3ktdsl.rot
import org.wysko.midis2jam2.jme3ktdsl.vec3
import org.wysko.midis2jam2.mapRangeClamped
import kotlin.time.Duration

abstract class Key(
    parent: KeyedInstrument,
    private val maximumKeyRotation: Float = 10f,
) {
    private var state: State = State.Up
    protected val root = node()

    init {
        parent.root += root
    }

    fun tick(delta: Duration) {
        root.rot = when (state) {
            is State.Down -> vec3(((state as State.Down).velocity / 127.0) * maximumKeyRotation, 0, 0)
            is State.Up -> vec3(interpTo(root.rot.x, 0f, delta.dSeconds, 20f), 0, 0)
        }
        root.depthFirstTraversal {
            if (it is Geometry) {
                it.material.setParam("AOIntensity", mapRangeClamped(root.rot.x, 0f, maximumKeyRotation, 1.0f, 0.33f))
            }
        }
    }

    open fun setState(newState: State) {
        state = newState
    }

    sealed interface State {
        data object Up : State
        data class Down(val velocity: Int) : State

        companion object {
            fun fromVelocity(velocity: Int): State = if (velocity < 1) Up else Down(velocity)
        }
    }
}